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
package org.rulii.spring.test.annotation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.rulii.registry.RuleRegistry;
import org.rulii.spring.annotation.RuleScan;
import org.rulii.spring.config.RuleRegistrar;
import org.rulii.spring.config.RuleRegistrarMetaInfo;
import org.rulii.spring.testrules.directimport.DirectImportConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that importing {@link RuleRegistrar} directly via {@code @Import} - without
 * {@link RuleScan} - behaves like a bare {@code @RuleScan}: the importing class's own
 * package is scanned and no XML locations are loaded.
 *
 * <p>Regression test: with no {@code @RuleScan} present,
 * {@code getAllAnnotationAttributes} returns {@code null} and the registrar previously
 * failed at startup with a raw {@link NullPointerException}.
 *
 * @author Max Arulananthan
 * @since 2.0
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = DirectImportConfig.class)
class RuleScanDirectImportTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private RuleRegistry ruleRegistry;

    @Autowired
    private RuleRegistrarMetaInfo metaInfo;

    @Test
    void contextStartsWithoutRuleScanAnnotation() {
        assertNotNull(context, "direct @Import(RuleRegistrar.class) must not fail at startup");
    }

    @Test
    void ownPackageIsScannedAsBareRuleScanFallback() {
        assertTrue(context.containsBean("DirectImportNeighborRule"));
        assertNotNull(ruleRegistry.getRule("DirectImportNeighborRule"));
    }

    @Test
    void metaInfoReflectsBareScanDefaults() {
        assertEquals(List.of("org.rulii.spring.testrules.directimport"), metaInfo.rulePackages());
        assertEquals(1, metaInfo.ruleCount(), "only the neighbor rule should have been registered");
        assertTrue(metaInfo.xmlLocations().isEmpty(), "no XML locations should have been loaded: "
                + metaInfo.xmlLocations());
    }
}
