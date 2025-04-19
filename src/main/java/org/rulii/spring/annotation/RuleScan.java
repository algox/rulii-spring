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
package org.rulii.spring.annotation;

import org.rulii.spring.config.RuleRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Annotation used to enable scanning for rule classes in specified base packages.
 * Rules found during scanning will be registered in the Spring application context.
 *
 * @author Max Arulananthan
 * @since 1.0
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(RuleRegistrar.class)
public @interface RuleScan {

    /**
     * Retrieve the base packages to be scanned for rule classes.
     *
     * @return an array of strings representing the base packages for scanning
     */
    String[] scanBasePackages() default {};
}


