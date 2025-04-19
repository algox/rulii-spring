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

import org.rulii.annotation.Rule;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.LinkedList;
import java.util.List;

/**
 * RuleBeanDefinitionScanner extends ClassPathBeanDefinitionScanner to scan for classes
 * annotated with @Rule in the specified base packages and register them in the Spring
 * application context.
 *
 * @author Max Arulananthan
 * @since 1.0
 *
 */
class RuleBeanDefinitionScanner extends ClassPathBeanDefinitionScanner {

    private boolean ruleScanStarted = false;
    private final List<BeanDefinitionHolder> ruleBeans = new LinkedList<>();

    RuleBeanDefinitionScanner() {
        super(new AnnotationConfigApplicationContext(), false);
        addIncludeFilter(new AnnotationTypeFilter(Rule.class));
    }

    /**
     * Scans for rules in the specified base packages and registers them in the Spring application context.
     *
     * @param basePackages an array of strings representing the base packages to scan for rule classes
     */
    void scanForRules(String...basePackages) {
        this.ruleScanStarted = true;
        scan(basePackages);
    }

    @Override
    protected void registerBeanDefinition(BeanDefinitionHolder definitionHolder, BeanDefinitionRegistry registry) {
        if (ruleScanStarted) ruleBeans.add(definitionHolder);
    }

    /**
     * Retrieve a list of BeanDefinitionHolders that represent rule beans.
     *
     * @return a List of BeanDefinitionHolders containing information about rule beans
     */
    public List<BeanDefinitionHolder> getRuleBeans() {
        return ruleBeans;
    }
}
