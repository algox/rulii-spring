/*
 * This software is licensed under the Apache 2 license, quoted below.
 *
 * Copyright (c) 1999-2026, Algorithmx Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rulii.spring.config;

import org.rulii.annotation.Rule;
import org.rulii.lib.spring.core.annotation.AnnotationUtils;
import org.rulii.model.UnrulyException;
import org.rulii.rule.ClassBasedRuleBuilder;
import org.rulii.spring.annotation.RuleScan;
import org.rulii.util.reflect.ObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * RuleRegistrar is a class that implements ImportBeanDefinitionRegistrar to register rule classes in the Spring application context.
 *
 * <p>The registrar is {@link EnvironmentAware} and {@link ResourceLoaderAware} so that
 * classpath scanning runs against the running application's {@link Environment} and
 * {@link ResourceLoader}, honoring {@code @Profile} / {@code @Conditional} on rule classes.
 *
 * <p>Importing this class directly via {@code @Import(RuleRegistrar.class)} (without
 * {@code @RuleScan}) behaves like a bare {@code @RuleScan}: the importing class's own
 * package is scanned and no XML locations are loaded.
 *
 * @author Max Arulananthan
 * @since 1.0
 */
public class RuleRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware, ResourceLoaderAware {
    private static final Logger LOGGER = LoggerFactory.getLogger(RuleRegistrar.class);

    private Environment environment;
    private ResourceLoader resourceLoader;

    public RuleRegistrar() {
        super();
    }

    /**
     * Constructs a new {@code RuleRegistrar} with the given environment and resource loader.
     * Used when the registrar is created programmatically (outside Spring's
     * {@code @Import} machinery, which supplies both via the aware callbacks).
     *
     * @param environment    the environment used to evaluate {@code @Profile} / {@code @Conditional}
     * @param resourceLoader the resource loader used for classpath scanning
     */
    public RuleRegistrar(Environment environment, ResourceLoader resourceLoader) {
        super();
        this.environment = environment;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    private Environment getEnvironment() {
        if (environment == null) environment = new StandardEnvironment();
        return environment;
    }

    private ResourceLoader getResourceLoader() {
        if (resourceLoader == null) resourceLoader = new DefaultResourceLoader();
        return resourceLoader;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry, BeanNameGenerator importBeanNameGenerator) {
        MultiValueMap<String, Object> attributes = importingClassMetadata.getAllAnnotationAttributes(RuleScan.class.getName());

        String[] xmlLocations = getAttributes(attributes, "xmlLocations");
        if (xmlLocations == null) xmlLocations = new String[0];

        // 1. Class-based rules are registered first (scanBasePackages takes priority)
        String[] rulePackages = getPackageNamesForScanning(getAttributes(attributes, "scanBasePackages"), xmlLocations, importingClassMetadata.getClassName());
        int count = registerRules(rulePackages, registry);

        // 2. XML-declared rules are loaded afterwards
        loadXmlContexts(xmlLocations, registry);

        // Register the Meta-Info
        registerMetaInfo(rulePackages, xmlLocations, count, registry);
        // Marker disables RuleConfig's auto-scan fallback
        registerScanMarker(registry);
    }

    /**
     * Registers the internal {@link RuleScanMarker} bean signalling that {@code @RuleScan}
     * has been processed. Idempotent - multiple {@code @RuleScan} configuration classes
     * share a single marker.
     *
     * @param registry the BeanDefinitionRegistry where the marker will be registered
     */
    private void registerScanMarker(BeanDefinitionRegistry registry) {
        if (registry.containsBeanDefinition(BeanNames.RULE_SCAN_MARKER)) return;
        registry.registerBeanDefinition(BeanNames.RULE_SCAN_MARKER,
                BeanDefinitionBuilder.genericBeanDefinition(RuleScanMarker.class).getBeanDefinition());
    }

    /**
     * Register rules from the specified packages into the given BeanDefinitionRegistry.
     *
     * @param rulePackages an array of strings representing the packages to scan for rule classes
     * @param registry the BeanDefinitionRegistry where the rules will be registered
     * @return the total number of rules successfully registered
     */
    public int registerRules(String[] rulePackages, BeanDefinitionRegistry registry) {
        if (rulePackages.length == 0) return 0;

        RuleBeanDefinitionScanner scanner = new RuleBeanDefinitionScanner(getEnvironment(), getResourceLoader());
        LOGGER.info("Scanning for Rules under  " + Arrays.toString(rulePackages));

        int result = 0;
        for (String rulePackage : rulePackages) {
            for (BeanDefinition candidate : scanner.findCandidateComponents(rulePackage)) {
                boolean registered = registerRule(candidate, registry);

                if (registered && LOGGER.isDebugEnabled()) LOGGER.debug("Registering Rule [" + candidate.getBeanClassName() + "]");
                if (registered) result++;
            }
        }

        LOGGER.info("Rule registration complete. Found [" + result + "] rule(s).");
        return result;
    }

    /**
     * Register a rule with the specified bean definition and registry.
     *
     * @param beanDefinition the BeanDefinition of the rule
     * @param registry the BeanDefinitionRegistry where the rule will be registered
     * @return true if the rule was successfully registered, false otherwise
     */
    private boolean registerRule(BeanDefinition beanDefinition, BeanDefinitionRegistry registry) {
        Class<?> ruleClass = getClass(beanDefinition.getBeanClassName());

        if (ruleClass == null) {
            LOGGER.warn("Could not register Rule. Unable to load Rule Class [" + beanDefinition.getBeanClassName() + "]");
            return false;
        }

        if (AnnotationUtils.getAnnotation(ruleClass, Rule.class) == null) {
            // Not a Rule
            LOGGER.warn("Could not register Rule. Rule Class [" + beanDefinition.getBeanClassName() + "] is not annotated with @Rule.");
            return false;
        }

        String ruleName = ClassBasedRuleBuilder.getRuleName(ruleClass);

        if (registry.containsBeanDefinition(ruleName)) {
            Class<?> existingRuleClass = getExistingRuleClass(registry.getBeanDefinition(ruleName));

            if (ruleClass.equals(existingRuleClass)) {
                // Same class discovered again (e.g. overlapping scanBasePackages)
                if (LOGGER.isDebugEnabled()) LOGGER.debug("Rule [" + ruleClass.getName() + "] is already registered. Skipping duplicate.");
                return false;
            }

            throw new UnrulyException("Duplicate rule name [" + ruleName + "]. "
                    + (existingRuleClass != null ? "Rule class [" + existingRuleClass.getName() + "]" : "An existing bean definition")
                    + " and Rule class [" + ruleClass.getName() + "] resolve to the same name."
                    + " Use @Rule(name = ...) to give one of them a unique name.");
        }

        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(RuleBeanBuilder.class);
        builder.addConstructorArgValue(ruleClass);
        // Reference by type (not by name) so an application-supplied ObjectFactory bean,
        // registered under any name, is honored - RuleConfig's bean is conditional on the type.
        builder.addConstructorArgValue(new RuntimeBeanReference(ObjectFactory.class));
        builder.setFactoryMethod("build");
        registry.registerBeanDefinition(ruleName, builder.getBeanDefinition());

        return true;
    }

    /**
     * Extracts the rule class from a bean definition previously registered by this registrar.
     *
     * @param beanDefinition an existing bean definition registered under a rule name
     * @return the rule class, or {@code null} if the definition was not registered by this registrar
     */
    private Class<?> getExistingRuleClass(BeanDefinition beanDefinition) {
        if (!RuleBeanBuilder.class.getName().equals(beanDefinition.getBeanClassName())) return null;
        ConstructorArgumentValues.ValueHolder holder = beanDefinition.getConstructorArgumentValues().getIndexedArgumentValue(0, null);
        return holder != null && holder.getValue() instanceof Class<?> ruleClass ? ruleClass : null;
    }

    /**
     * Loads XML rule context files from each of the given locations and registers their
     * bean definitions directly into the supplied registry.
     *
     * <p>Each location may take one of three forms:
     * <ul>
     *   <li><b>Folder</b> (e.g. {@code classpath:rules/pricing/}) — all {@code *.xml} files
     *       directly inside it are loaded; a {@code classpath:} prefix is upgraded to
     *       {@code classpath*:} so all classpath roots are searched</li>
     *   <li><b>File</b> (ends with {@code .xml}) — that file is loaded; a missing file
     *       fails fast with an {@link UnrulyException}</li>
     *   <li><b>Pattern</b> (contains {@code *}) — used as-is for resource resolution</li>
     * </ul>
     *
     * <p>The {@link XmlBeanDefinitionReader} uses the same {@link BeanDefinitionRegistry}, so
     * all beans from the XML files land in the same application context as class-scanned
     * rules. The rulii namespace handler is picked up automatically via
     * {@code META-INF/spring.handlers}.
     *
     * @param xmlLocations XML locations declared on {@code @RuleScan}
     * @param registry     the registry to load bean definitions into
     */
    private void loadXmlContexts(String[] xmlLocations, BeanDefinitionRegistry registry) {
        if (xmlLocations.length == 0) return;

        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);
        // The registry alone is not EnvironmentCapable; without these the reader falls back
        // to a fresh StandardEnvironment and silently skips <beans profile="..."> sections.
        reader.setEnvironment(getEnvironment());
        reader.setResourceLoader(getResourceLoader());
        ResourcePatternResolver resolver = ResourcePatternUtils.getResourcePatternResolver(getResourceLoader());

        for (String location : xmlLocations) {
            String pattern = toResourcePattern(location);
            boolean explicitFile = location.endsWith(".xml") && !location.contains("*");

            try {
                Resource[] resources = resolver.getResources(pattern);

                if (explicitFile && (resources.length == 0 || !resources[0].exists())) {
                    throw new UnrulyException("XML rule context file not found [" + location + "]");
                }

                if (resources.length == 0) {
                    LOGGER.warn("No XML rule context files found at location [" + location + "]");
                    continue;
                }

                for (Resource resource : resources) {
                    LOGGER.info("Loading XML rule context [" + resource.getDescription() + "]");
                    reader.loadBeanDefinitions(resource);
                }
            } catch (IOException e) {
                throw new UnrulyException("Failed to resolve XML rule context resources at location [" + location + "]", e);
            }
        }
    }

    /**
     * Converts a {@code @RuleScan} XML location into a resource pattern.
     *
     * <p>Direct files (ending with {@code .xml}) and user-supplied patterns (containing
     * {@code *}) are used as-is. Anything else is treated as a folder: normalised to end
     * with {@code /} and suffixed with {@code *.xml}. Folder scans upgrade a
     * {@code classpath:} prefix to {@code classpath*:} so files in all classpath roots
     * (e.g. rule sets shipped in separate jars) are found - the same semantics as
     * component scanning. Other prefixes are left untouched.
     *
     * @param location an XML location declared on {@code @RuleScan}
     * @return the resource pattern to resolve
     */
    private String toResourcePattern(String location) {
        if (location.endsWith(".xml") || location.contains("*")) return location;

        String folder = location.endsWith("/") ? location : location + "/";

        if (folder.startsWith(ResourceLoader.CLASSPATH_URL_PREFIX)) {
            folder = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
                    + folder.substring(ResourceLoader.CLASSPATH_URL_PREFIX.length());
        }

        return folder + "*.xml";
    }

    /**
     * Register the meta information for rules in the given packages into the provided BeanDefinitionRegistry,
     * under the fixed name {@link BeanNames#RULE_SCAN_META_INFO}.
     *
     * <p>When multiple {@code @RuleScan} configuration classes are present, the meta-info from each
     * scan is merged (union of packages and XML locations, summed rule count) so the application
     * always sees exactly one injectable {@link RuleRegistrarMetaInfo} bean.
     *
     * @param rulePackages an array of strings representing the packages to scan for rule classes
     * @param xmlLocations an array of locations from which XML context files were loaded
     * @param ruleCount    the total number of rules successfully registered
     * @param registry     the BeanDefinitionRegistry where the meta information will be registered
     */
    private void registerMetaInfo(String[] rulePackages, String[] xmlLocations, int ruleCount, BeanDefinitionRegistry registry) {
        List<String> packages = Arrays.asList(rulePackages);
        List<String> locations = Arrays.asList(xmlLocations);

        if (registry.containsBeanDefinition(BeanNames.RULE_SCAN_META_INFO)) {
            BeanDefinition existing = registry.getBeanDefinition(BeanNames.RULE_SCAN_META_INFO);

            if (RuleRegistrarMetaInfo.class.getName().equals(existing.getBeanClassName())) {
                // A previous @RuleScan already registered meta-info - merge into a single bean
                packages = merge(getStringListArg(existing, 0), packages);
                locations = merge(getStringListArg(existing, 1), locations);
                ruleCount += getIntArg(existing, 2);
                registry.removeBeanDefinition(BeanNames.RULE_SCAN_META_INFO);
            }
        }

        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(RuleRegistrarMetaInfo.class);
        builder.addConstructorArgValue(packages);
        builder.addConstructorArgValue(locations);
        builder.addConstructorArgValue(ruleCount);
        registry.registerBeanDefinition(BeanNames.RULE_SCAN_META_INFO, builder.getBeanDefinition());
    }

    /**
     * Merges two string lists into one, preserving first-seen order and dropping duplicates.
     *
     * @param first  the existing values; may be null
     * @param second the values to append; may be null
     * @return the merged list
     */
    private List<String> merge(List<String> first, List<String> second) {
        Set<String> result = new LinkedHashSet<>();
        if (first != null) result.addAll(first);
        if (second != null) result.addAll(second);
        return List.copyOf(result);
    }

    /**
     * Retrieves a {@code List<String>} constructor argument from the given bean definition.
     *
     * @param definition the bean definition to read from
     * @param index      the constructor argument index
     * @return the argument value, or an empty list if absent or of a different type
     */
    private List<String> getStringListArg(BeanDefinition definition, int index) {
        ConstructorArgumentValues.ValueHolder holder = definition.getConstructorArgumentValues().getIndexedArgumentValue(index, null);
        return holder != null && holder.getValue() instanceof List<?> value
                ? value.stream().map(String.class::cast).toList()
                : List.of();
    }

    /**
     * Retrieves an int constructor argument from the given bean definition.
     *
     * @param definition the bean definition to read from
     * @param index      the constructor argument index
     * @return the argument value, or 0 if absent or of a different type
     */
    private int getIntArg(BeanDefinition definition, int index) {
        ConstructorArgumentValues.ValueHolder holder = definition.getConstructorArgumentValues().getIndexedArgumentValue(index, null);
        return holder != null && holder.getValue() instanceof Integer value ? value : 0;
    }

    /**
     * Get the package names for scanning based on the provided rule bean packages and import class name.
     *
     * <p>Declared {@code scanBasePackages} always win. When they are absent, the fallback to the
     * importing class's own package applies only if {@code xmlLocations} is also empty - an
     * {@code xmlLocations}-only {@code @RuleScan} is XML-only and performs no class scan.
     *
     * @param ruleBeanPackages an array of strings representing the rule bean packages
     * @param xmlLocations the XML locations declared on the annotation
     * @param importClassName the class name used for import
     * @return an array of strings representing the package names for scanning; empty for XML-only scans
     */
    private String[] getPackageNamesForScanning(String[] ruleBeanPackages, String[] xmlLocations, String importClassName) {
        if (ruleBeanPackages != null && ruleBeanPackages.length > 0) return ruleBeanPackages;
        if (xmlLocations.length > 0) return new String[] {};

        Class<?> importClass = getClass(importClassName);

        if (importClass == null) throw new UnrulyException("Unable to load class [" + importClassName + "]");
        return new String[] {importClass.getPackageName()};
    }

    /**
     * Retrieves the Class object for the given class name. If the class is found, it will return the Class object, otherwise it will return null.
     *
     * @param className the name of the class to retrieve
     * @return the Class object for the specified class name, or null if the class is not found
     */
    private Class<?> getClass(String className) {
        try {
            return ClassUtils.forName(className, null);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * Retrieves the attributes associated with a given name from a MultiValueMap.
     *
     * @param attributes the MultiValueMap containing the attributes; null when the importing
     *                   class was not annotated with {@code @RuleScan} (direct {@code @Import})
     * @param name the name of the attribute to retrieve
     * @return an array of strings representing the attributes associated with the given name, or null if not found
     */
    private String[] getAttributes(MultiValueMap<String, Object> attributes, String name) {
        return attributes != null && attributes.containsKey(name) ? (String[]) attributes.getFirst(name) : null;
    }
}
