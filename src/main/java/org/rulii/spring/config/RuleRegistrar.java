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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.util.Arrays;

/**
 * RuleRegistrar is a class that implements ImportBeanDefinitionRegistrar to register rule classes in the Spring application context.
 *
 * @author Max Arulananthan
 * @since 1.0
 */
public class RuleRegistrar implements ImportBeanDefinitionRegistrar {
    private static final Logger LOGGER = LoggerFactory.getLogger(RuleRegistrar.class);

    public RuleRegistrar() {
        super();
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry, BeanNameGenerator importBeanNameGenerator) {
        MultiValueMap<String, Object> attributes = importingClassMetadata.getAllAnnotationAttributes(RuleScan.class.getName());

        // 1. Class-based rules are registered first (scanBasePackages takes priority)
        String[] rulePackages = getPackageNamesForScanning(getAttributes(attributes, "scanBasePackages"), importingClassMetadata.getClassName());
        int count = registerRules(rulePackages, registry);

        // 2. XML-declared rules are loaded afterwards
        String[] xmlLocations = getAttributes(attributes, "xmlLocations");
        if (xmlLocations == null) xmlLocations = new String[0];
        loadXmlContexts(xmlLocations, registry);

        // Register the Meta-Info
        registerMetaInfo(rulePackages, xmlLocations, count, registry, importBeanNameGenerator);
    }

    /**
     * Register rules from the specified packages into the given BeanDefinitionRegistry.
     *
     * @param rulePackages an array of strings representing the packages to scan for rule classes
     * @param registry the BeanDefinitionRegistry where the rules will be registered
     * @return the total number of rules successfully registered
     */
    public int registerRules(String[] rulePackages, BeanDefinitionRegistry registry) {
        RuleBeanDefinitionScanner scanner = new RuleBeanDefinitionScanner();
        LOGGER.info("Scanning for Rules under  " + Arrays.toString(rulePackages));
        scanner.scanForRules(rulePackages);

        int result = 0;
        for (BeanDefinitionHolder holder : scanner.getRuleBeans()) {
            boolean registered = registerRule(holder.getBeanName(), holder.getBeanDefinition(), registry);

            if (registered && LOGGER.isDebugEnabled()) LOGGER.debug("Registering Rule [" + holder.getBeanDefinition().getBeanClassName() + "]");
            if (registered) result++;
        }

        LOGGER.info("Rule registration complete. Found [" + result + "] rule(s).");
        return result;
    }

    /**
     * Register a rule with the specified bean name, bean definition, and registry.
     *
     * @param beanName the name of the bean to register
     * @param beanDefinition the BeanDefinition of the rule
     * @param registry the BeanDefinitionRegistry where the rule will be registered
     * @return true if the rule was successfully registered, false otherwise
     */
    private boolean registerRule(String beanName, BeanDefinition beanDefinition, BeanDefinitionRegistry registry) {
        Class<?> ruleClass = getClass(beanDefinition.getBeanClassName());

        if (ruleClass == null) {
            LOGGER.warn("Could not register [" + beanName + "]. Unable to load Rule Class [" + beanDefinition.getBeanClassName() + "]");
            return false;
        }

        if (AnnotationUtils.getAnnotation(ruleClass, Rule.class) == null) {
            // Not a Rule
            LOGGER.warn("Could not register [" + beanName + "]. Rule Class [" + beanDefinition.getBeanClassName() + "] is not annotated with @Rule.");
            return false;
        }

        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(RuleBeanBuilder.class);
        builder.addConstructorArgValue(ruleClass);
        builder.addConstructorArgReference(BeanNames.OBJECT_FACTORY);
        builder.setFactoryMethod("build");
        registry.registerBeanDefinition(ClassBasedRuleBuilder.getRuleName(ruleClass), builder.getBeanDefinition());

        return true;
    }

    /**
     * Loads all {@code *.xml} files from each of the given classpath folder locations and
     * registers their bean definitions directly into the supplied registry.
     *
     * <p>Each location is treated as a folder: the path is normalised to end with {@code /}
     * and {@code *.xml} is appended before resource resolution. The
     * {@link XmlBeanDefinitionReader} uses the same {@link BeanDefinitionRegistry}, so
     * all beans from the XML files land in the same application context as class-scanned
     * rules. The rulii namespace handler is picked up automatically via
     * {@code META-INF/spring.handlers}.
     *
     * @param xmlLocations classpath folder locations declared on {@code @RuleScan}
     * @param registry     the registry to load bean definitions into
     */
    private void loadXmlContexts(String[] xmlLocations, BeanDefinitionRegistry registry) {
        if (xmlLocations.length == 0) return;

        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        for (String location : xmlLocations) {
            String pattern = location.endsWith("/") ? location + "*.xml" : location + "/*.xml";

            try {
                Resource[] resources = resolver.getResources(pattern);

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
     * Register the meta information for rules in the given packages into the provided BeanDefinitionRegistry.
     *
     * @param rulePackages              an array of strings representing the packages to scan for rule classes
     * @param xmlLocations              an array of classpath folder locations from which XML context files were loaded
     * @param ruleCount                 the total number of rules successfully registered
     * @param registry                  the BeanDefinitionRegistry where the meta information will be registered
     * @param importBeanNameGenerator   the BeanNameGenerator for generating bean names
     */
    private void registerMetaInfo(String[] rulePackages, String[] xmlLocations, int ruleCount, BeanDefinitionRegistry registry, BeanNameGenerator importBeanNameGenerator) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(RuleRegistrarMetaInfo.class);
        builder.addConstructorArgValue(rulePackages);
        builder.addConstructorArgValue(xmlLocations);
        builder.addConstructorArgValue(ruleCount);
        BeanDefinition definition = builder.getBeanDefinition();
        registry.registerBeanDefinition(importBeanNameGenerator.generateBeanName(definition, registry), definition);
    }

    /**
     * Get the package names for scanning based on the provided rule bean packages and import class name.
     *
     * @param ruleBeanPackages an array of strings representing the rule bean packages
     * @param importClassName the class name used for import
     * @return an array of strings representing the package names for scanning
     */
    private String[] getPackageNamesForScanning(String[] ruleBeanPackages, String importClassName) {
        if (ruleBeanPackages != null && ruleBeanPackages.length > 0) return ruleBeanPackages;

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
     * @param attributes the MultiValueMap containing the attributes
     * @param name the name of the attribute to retrieve
     * @return an array of strings representing the attributes associated with the given name, or null if not found
     */
    private String[] getAttributes(MultiValueMap<String, Object> attributes, String name) {
        return attributes.containsKey(name) ? (String[]) attributes.getFirst(name) : null;
    }
}
