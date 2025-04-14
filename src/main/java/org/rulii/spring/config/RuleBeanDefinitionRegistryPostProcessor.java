/*
 * This software is licensed under the Apache 2 license, quoted below.
 *
 * Copyright (c) 1999-2025, Algorithmx Inc.
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

public class RuleBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(RuleBeanDefinitionRegistryPostProcessor.class);
    private final List<String> basePackages;

    public RuleBeanDefinitionRegistryPostProcessor(List<String> basePackages) {
        super();
        this.basePackages = basePackages;
    }

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
