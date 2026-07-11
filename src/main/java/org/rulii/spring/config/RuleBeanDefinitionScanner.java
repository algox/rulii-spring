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
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;

/**
 * RuleBeanDefinitionScanner scans the classpath for classes annotated with {@link Rule}
 * so they can be registered in the Spring application context.
 *
 * <p>It runs against the running application's {@link Environment} and
 * {@link ResourceLoader}, so {@code @Profile} / {@code @Conditional} declarations on
 * rule classes are honored and no throwaway application context is created.
 *
 * @author Max Arulananthan
 * @since 1.0
 *
 */
class RuleBeanDefinitionScanner extends ClassPathScanningCandidateComponentProvider {

    /**
     * Constructs a new {@code RuleBeanDefinitionScanner} configured to include only
     * classes annotated with {@link Rule}.
     *
     * @param environment    the environment used to evaluate {@code @Profile} / {@code @Conditional}
     * @param resourceLoader the resource loader used to locate candidate classes
     */
    RuleBeanDefinitionScanner(Environment environment, ResourceLoader resourceLoader) {
        super(false, environment);
        if (resourceLoader != null) setResourceLoader(resourceLoader);
        addIncludeFilter(new AnnotationTypeFilter(Rule.class));
    }
}
