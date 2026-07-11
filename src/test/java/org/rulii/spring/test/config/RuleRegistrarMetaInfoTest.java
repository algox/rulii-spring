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
package org.rulii.spring.test.config;

import org.junit.jupiter.api.Test;
import org.rulii.spring.config.RuleRegistrarMetaInfo;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the value semantics and immutability of {@link RuleRegistrarMetaInfo}.
 *
 * <p>Regression test: the record previously held {@code String[]} components, so the
 * generated {@code equals}/{@code hashCode} compared arrays by reference and the
 * accessors exposed the singleton bean's internal state for mutation.
 *
 * @author Max Arulananthan
 * @since 2.0
 */
class RuleRegistrarMetaInfoTest {

    @Test
    void identicallyPopulatedInstancesAreEqual() {
        RuleRegistrarMetaInfo a = new RuleRegistrarMetaInfo(List.of("com.example"), List.of("classpath:rules/"), 3);
        RuleRegistrarMetaInfo b = new RuleRegistrarMetaInfo(List.of("com.example"), List.of("classpath:rules/"), 3);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void differentContentIsNotEqual() {
        RuleRegistrarMetaInfo a = new RuleRegistrarMetaInfo(List.of("com.example"), List.of(), 3);
        RuleRegistrarMetaInfo b = new RuleRegistrarMetaInfo(List.of("com.other"), List.of(), 3);

        assertNotEquals(a, b);
    }

    @Test
    void accessorsReturnUnmodifiableLists() {
        RuleRegistrarMetaInfo metaInfo = new RuleRegistrarMetaInfo(List.of("com.example"), List.of(), 1);

        assertThrows(UnsupportedOperationException.class, () -> metaInfo.rulePackages().add("hacked"));
        assertThrows(UnsupportedOperationException.class, () -> metaInfo.xmlLocations().add("hacked"));
    }

    @Test
    void constructorTakesDefensiveCopies() {
        List<String> packages = new ArrayList<>(List.of("com.example"));
        RuleRegistrarMetaInfo metaInfo = new RuleRegistrarMetaInfo(packages, new ArrayList<>(), 1);

        packages.add("com.injected");
        assertEquals(List.of("com.example"), metaInfo.rulePackages());
    }
}
