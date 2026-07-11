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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

import java.util.List;

/**
 * A BeanDefinitionRegistryPostProcessor implementation for handling registration of Rule beans in the Spring application context.
 * This class scans for Rule classes in specified base packages and registers them in the BeanDefinitionRegistry.
 *
 * @author Max Arulananthan
 * @since 1.0
 *
 */
public class RuleBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuleBeanDefinitionRegistryPostProcessor.class);

    private final List<String> basePackages;
    private final Environment environment;
    private final ResourceLoader resourceLoader;

    /**
     * Constructs a new {@code RuleBeanDefinitionRegistryPostProcessor} with the given base packages.
     *
     * @param basePackages   the list of base packages to scan for {@code @Rule}-annotated classes;
     *                       may be {@code null} or empty, in which case no rules are registered
     * @param environment    the environment used to evaluate {@code @Profile} / {@code @Conditional} on rule classes
     * @param resourceLoader the resource loader used for classpath scanning
     */
    RuleBeanDefinitionRegistryPostProcessor(List<String> basePackages, Environment environment, ResourceLoader resourceLoader) {
        super();
        this.basePackages = basePackages;
        this.environment = environment;
        this.resourceLoader = resourceLoader;
    }

    /**
     * Scans the configured base packages for {@code @Rule}-annotated classes and registers
     * each discovered rule as a bean definition in the provided registry, followed by a
     * {@link RuleRegistrarMetaInfo} bean describing the scan (mirroring the {@code @RuleScan}
     * path, so meta-info is injectable regardless of which path configured the application).
     *
     * <p>If no base packages are configured, a warning is logged and registration is skipped.</p>
     *
     * @param registry the {@link BeanDefinitionRegistry} to register rule bean definitions into
     * @throws BeansException if bean definition registration fails
     */
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {

        if (basePackages == null || basePackages.isEmpty()) {
            LOGGER.warn("Unable to auto-register Rules. Could not detect base package to scan.");
            return;
        }

        RuleRegistrar registrar = new RuleRegistrar(environment, resourceLoader);
        int count = registrar.registerRules(basePackages.toArray(new String[] {}), registry);

        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(RuleRegistrarMetaInfo.class);
        builder.addConstructorArgValue(List.copyOf(basePackages));
        builder.addConstructorArgValue(List.of());
        builder.addConstructorArgValue(count);
        registry.registerBeanDefinition(BeanNames.RULE_SCAN_META_INFO, builder.getBeanDefinition());
    }
}
