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

import org.rulii.rule.Rule;
import org.rulii.util.reflect.ObjectFactory;

/**
 * RuleBeanBuilder is a final class that provides a static method to build a Rule object based on the input ruleClass and objectFactory.
 *
 * @author Max Arulananthan
 * @since 1.0
 *
 */
final class RuleBeanBuilder {

    private RuleBeanBuilder() {
        super();
    }

    /**
     * Build a Rule object based on the input ruleClass and ObjectFactory.
     *
     * @param ruleClass the class representing the rule
     * @param objectFactory the factory for creating rule objects
     * @return a Rule object built using the specified ruleClass and objectFactory
     */
    static Rule build(Class<?> ruleClass, ObjectFactory objectFactory) {
        return Rule.builder().build(ruleClass, objectFactory);
    }
}
