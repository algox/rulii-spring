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
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;

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

    /**
     * Constructs a new {@code RuleBeanDefinitionRegistryPostProcessor} with the given base packages.
     *
     * @param basePackages the list of base packages to scan for {@code @Rule}-annotated classes;
     *                     may be {@code null} or empty, in which case no rules are registered
     */
    RuleBeanDefinitionRegistryPostProcessor(List<String> basePackages) {
        super();
        this.basePackages = basePackages;
    }

    /**
     * Scans the configured base packages for {@code @Rule}-annotated classes and registers
     * each discovered rule as a bean definition in the provided registry.
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

        RuleRegistrar registrar = new RuleRegistrar();
        registrar.registerRules(basePackages.toArray(new String[] {}), registry);
    }
}
