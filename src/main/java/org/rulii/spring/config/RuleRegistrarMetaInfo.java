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

import java.util.List;

/**
 * Represents meta information about the rules registered within a rule registrar.
 * Contains the rule packages, XML locations, and the total count of rules registered.
 *
 * <p>A single instance of this record is registered as a Spring bean by {@link RuleRegistrar}
 * after rule scanning completes. It can be injected into application components to inspect
 * which packages were scanned, which XML locations were loaded, and how many rules were found.</p>
 *
 * <p>The record is immutable: the lists are unmodifiable defensive copies, and equality
 * is value-based.</p>
 *
 * @param rulePackages the packages that were scanned for {@code @Rule}-annotated classes
 * @param xmlLocations the locations declared on {@code @RuleScan(xmlLocations)}, as written
 * @param xmlFiles     the XML files those locations resolved to and that were loaded, in load
 *                     order, as resource descriptions (since 2.1)
 * @param ruleCount    the total number of rules successfully registered during scanning
 *
 * @author Max Arulananthan
 * @since 1.0
 *
 */
public record RuleRegistrarMetaInfo(List<String> rulePackages, List<String> xmlLocations, List<String> xmlFiles,
                                    int ruleCount) {

    public RuleRegistrarMetaInfo {
        rulePackages = List.copyOf(rulePackages);
        xmlLocations = List.copyOf(xmlLocations);
        xmlFiles = List.copyOf(xmlFiles);
    }

    /**
     * Creates meta info without resolved XML files (pre-2.1 shape).
     *
     * @param rulePackages the scanned packages
     * @param xmlLocations the declared XML locations
     * @param ruleCount    the number of rules registered
     */
    public RuleRegistrarMetaInfo(List<String> rulePackages, List<String> xmlLocations, int ruleCount) {
        this(rulePackages, xmlLocations, List.of(), ruleCount);
    }
}
