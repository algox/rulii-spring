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
import org.rulii.spring.config.RuleRegistrarMetaInfo;
import org.rulii.spring.testrules.xmlonly.XmlOnlyScanConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that an {@code xmlLocations}-only {@link RuleScan} is truly XML-only:
 * no class scanning is performed, not even of the config class's own package.
 *
 * <p>Regression test: with no {@code scanBasePackages}, the registrar previously fell
 * back to scanning the annotated class's package even when {@code xmlLocations} was
 * declared, silently registering any co-located {@code @Rule} classes.
 * {@link XmlOnlyScanConfig} deliberately shares a package with
 * {@code XmlOnlyNeighborRule}, which must stay unregistered.
 *
 * @author Max Arulananthan
 * @since 2.0
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = XmlOnlyScanConfig.class)
class RuleScanXmlOnlyNoClassScanTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private RuleRegistry ruleRegistry;

    @Autowired
    private RuleRegistrarMetaInfo metaInfo;

    @Test
    void xmlDeclaredRulesAreRegistered() {
        assertNotNull(ruleRegistry.getRule("XmlDeclaredAgeRule"));
        assertNotNull(ruleRegistry.getRule("XmlDeclaredPositiveRule"));
        assertNotNull(ruleRegistry.getRuleSet("XmlDeclaredRuleSet"));
    }

    @Test
    void neighborRuleClassIsNotScanned() {
        assertFalse(context.containsBean("XmlOnlyNeighborRule"),
                "xmlLocations-only @RuleScan must not scan the config class's own package");
        assertNull(ruleRegistry.getRule("XmlOnlyNeighborRule"));
    }

    @Test
    void metaInfoReflectsXmlOnlyScan() {
        assertTrue(metaInfo.rulePackages().isEmpty(), "no packages should have been class-scanned");
        assertEquals(0, metaInfo.ruleCount(), "no class-based rules should have been registered");
        assertTrue(metaInfo.xmlLocations().contains("classpath:rules/xml-scan/"));
    }
}
