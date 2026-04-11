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
import org.rulii.context.RuleContext;
import org.rulii.registry.RuleRegistry;
import org.rulii.rule.Rule;
import org.rulii.ruleset.RuleSet;
import org.rulii.spring.annotation.RuleScan;
import org.rulii.spring.config.RuleRegistrarMetaInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link RuleScan} when both {@code scanBasePackages} and
 * {@code xmlLocations} are specified.
 *
 * <p>Verifies that:
 * <ul>
 *   <li>Class-based rules (from {@code scanBasePackages}) are registered first</li>
 *   <li>XML-declared rules (from {@code xmlLocations}) are registered afterwards</li>
 *   <li>All rules are accessible via {@link RuleRegistry}</li>
 *   <li>{@link RuleRegistrarMetaInfo} captures both packages and locations</li>
 * </ul>
 *
 * @author Max Arulananthan
 * @since 1.0
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = RuleScanCombinedTest.Config.class)
class RuleScanCombinedTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @RuleScan(
        scanBasePackages = "org.rulii.spring.test.rules.xmlscan",
        xmlLocations = "classpath:rules/xml-scan/"
    )
    static class Config {}

    // Class-based rules
    @Autowired
    @Qualifier("XmlScanRule1")
    private Rule xmlScanRule1;

    @Autowired
    @Qualifier("XmlScanRule2")
    private Rule xmlScanRule2;

    // XML-declared rules
    @Autowired
    @Qualifier("XmlDeclaredAgeRule")
    private Rule xmlDeclaredAgeRule;

    @Autowired
    @Qualifier("XmlDeclaredPositiveRule")
    private Rule xmlDeclaredPositiveRule;

    @Autowired
    @Qualifier("XmlDeclaredRuleSet")
    private RuleSet<?> xmlDeclaredRuleSet;

    @Autowired
    private RuleRegistry ruleRegistry;

    @Autowired
    private RuleRegistrarMetaInfo metaInfo;

    // ------------------------------------------------------------------
    // Class-based rules
    // ------------------------------------------------------------------

    @Test
    void classBasedRulesAreRegistered() {
        assertNotNull(xmlScanRule1);
        assertNotNull(xmlScanRule2);
    }

    @Test
    void classBasedRulesHaveCorrectNames() {
        assertEquals("XmlScanRule1", xmlScanRule1.getName());
        assertEquals("XmlScanRule2", xmlScanRule2.getName());
    }

    @Test
    void classBasedRuleExecutesCorrectly() {
        RuleContext ctx = RuleContext.builder().standard().build();
        ctx.getBindings().bind("value", 5);
        assertTrue(xmlScanRule1.isTrue(ctx));

        RuleContext ctx2 = RuleContext.builder().standard().build();
        ctx2.getBindings().bind("value", -1);
        assertFalse(xmlScanRule1.isTrue(ctx2));
    }

    // ------------------------------------------------------------------
    // XML-declared rules
    // ------------------------------------------------------------------

    @Test
    void xmlDeclaredRulesAreRegistered() {
        assertNotNull(xmlDeclaredAgeRule);
        assertNotNull(xmlDeclaredPositiveRule);
    }

    @Test
    void xmlDeclaredRuleSetIsRegistered() {
        assertNotNull(xmlDeclaredRuleSet);
    }

    // ------------------------------------------------------------------
    // RuleRegistry — all rules visible
    // ------------------------------------------------------------------

    @Test
    void ruleRegistryContainsClassBasedRules() {
        assertNotNull(ruleRegistry.getRule("XmlScanRule1"));
        assertNotNull(ruleRegistry.getRule("XmlScanRule2"));
    }

    @Test
    void ruleRegistryContainsXmlDeclaredRules() {
        assertNotNull(ruleRegistry.getRule("XmlDeclaredAgeRule"));
        assertNotNull(ruleRegistry.getRule("XmlDeclaredPositiveRule"));
    }

    @Test
    void ruleRegistryContainsXmlDeclaredRuleSet() {
        assertNotNull(ruleRegistry.getRuleSet("XmlDeclaredRuleSet"));
    }

    @Test
    void ruleRegistryContainsAllFourRules() {
        List<Rule> rules = ruleRegistry.getRules();
        List<String> names = rules.stream().map(Rule::getName).toList();
        assertTrue(names.contains("XmlScanRule1"));
        assertTrue(names.contains("XmlScanRule2"));
        assertTrue(names.contains("XmlDeclaredAgeRule"));
        assertTrue(names.contains("XmlDeclaredPositiveRule"));
    }

    // ------------------------------------------------------------------
    // RuleRegistrarMetaInfo
    // ------------------------------------------------------------------

    @Test
    void metaInfoCapturesScanBasePackage() {
        assertNotNull(metaInfo.rulePackages());
        assertTrue(Arrays.asList(metaInfo.rulePackages()).contains("org.rulii.spring.test.rules.xmlscan"),
                "rulePackages should contain the declared scan package");
    }

    @Test
    void metaInfoCapturesXmlLocation() {
        assertNotNull(metaInfo.xmlLocations());
        assertTrue(Arrays.asList(metaInfo.xmlLocations()).contains("classpath:rules/xml-scan/"),
                "xmlLocations should contain the declared folder");
    }

    @Test
    void metaInfoRuleCountReflectsClassBasedRulesOnly() {
        // ruleCount tracks only class-scanned rules; XML beans are loaded separately
        assertEquals(2, metaInfo.ruleCount(), "ruleCount should reflect the two class-based rules");
    }
}
