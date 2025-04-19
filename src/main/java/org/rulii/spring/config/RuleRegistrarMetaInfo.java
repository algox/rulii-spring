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

import java.util.Arrays;

/**
 * Represents meta information about the rules registered within a rule registrar.
 * Contains the rule packages and the total count of rules registered.
 *
 * @author Max Arulananthan
 * @since 1.0
 *
 */
public record RuleRegistrarMetaInfo(String[] rulePackages, int ruleCount) {

    @Override
    public String toString() {
        return "RuleRegistrarMetaInfo{" +
                "rulePackages=" + Arrays.toString(rulePackages) +
                ", ruleCount=" + ruleCount +
                '}';
    }
}
