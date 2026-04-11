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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the {@code xmlLocations} attribute of {@link RuleScan}.
 *
 * <p>Verifies that XML rule context files found under the declared folder are
 * loaded and their bean definitions registered in the application context.
 * No class-based scanning is performed ({@code scanBasePackages} is left empty).
 *
 * @author Max Arulananthan
 * @since 1.0
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = RuleScanXmlLocationsTest.Config.class)
class RuleScanXmlLocationsTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @RuleScan(xmlLocations = "classpath:rules/xml-scan/")
    static class Config {}

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
    // Bean registration
    // ------------------------------------------------------------------

    @Test
    void xmlDeclaredRulesAreRegisteredAsBeans() {
        assertNotNull(xmlDeclaredAgeRule);
        assertNotNull(xmlDeclaredPositiveRule);
    }

    @Test
    void xmlDeclaredRuleSetIsRegisteredAsBean() {
        assertNotNull(xmlDeclaredRuleSet);
    }

    @Test
    void xmlDeclaredRulesHaveCorrectNames() {
        assertEquals("XmlDeclaredAgeRule", xmlDeclaredAgeRule.getName());
        assertEquals("XmlDeclaredPositiveRule", xmlDeclaredPositiveRule.getName());
    }

    @Test
    void xmlDeclaredRuleSetHasCorrectName() {
        assertEquals("XmlDeclaredRuleSet", xmlDeclaredRuleSet.getName());
    }

    // ------------------------------------------------------------------
    // Rule execution
    // ------------------------------------------------------------------

    @Test
    void xmlDeclaredAgeRulePassesWhenAgeIsAtBoundary() {
        RuleContext ctx = RuleContext.builder().standard().build();
        ctx.getBindings().bind("age", 18);
        assertTrue(xmlDeclaredAgeRule.isTrue(ctx));
    }

    @Test
    void xmlDeclaredAgeRuleFailsWhenAgeBelowBoundary() {
        RuleContext ctx = RuleContext.builder().standard().build();
        ctx.getBindings().bind("age", 17);
        assertFalse(xmlDeclaredAgeRule.isTrue(ctx));
    }

    @Test
    void xmlDeclaredPositiveRulePassesForPositiveValue() {
        RuleContext ctx = RuleContext.builder().standard().build();
        ctx.getBindings().bind("value", 1);
        assertTrue(xmlDeclaredPositiveRule.isTrue(ctx));
    }

    @Test
    void xmlDeclaredPositiveRuleFailsForZero() {
        RuleContext ctx = RuleContext.builder().standard().build();
        ctx.getBindings().bind("value", 0);
        assertFalse(xmlDeclaredPositiveRule.isTrue(ctx));
    }

    @Test
    void xmlDeclaredRuleSetIsExecutable() {
        RuleContext ctx = RuleContext.builder().standard().build();
        ctx.getBindings().bind("age", 21);
        assertDoesNotThrow(() -> xmlDeclaredRuleSet.run(ctx));
    }

    // ------------------------------------------------------------------
    // RuleRegistry
    // ------------------------------------------------------------------

    @Test
    void xmlDeclaredRulesAreAccessibleViaRuleRegistry() {
        assertNotNull(ruleRegistry.getRule("XmlDeclaredAgeRule"));
        assertNotNull(ruleRegistry.getRule("XmlDeclaredPositiveRule"));
    }

    @Test
    void xmlDeclaredRuleSetIsAccessibleViaRuleRegistry() {
        assertNotNull(ruleRegistry.getRuleSet("XmlDeclaredRuleSet"));
    }

    // ------------------------------------------------------------------
    // RuleRegistrarMetaInfo
    // ------------------------------------------------------------------

    @Test
    void metaInfoIsRegistered() {
        assertNotNull(metaInfo);
    }

    @Test
    void metaInfoCapturesXmlLocation() {
        assertNotNull(metaInfo.xmlLocations());
        assertTrue(Arrays.asList(metaInfo.xmlLocations()).contains("classpath:rules/xml-scan/"),
                "xmlLocations should contain the declared folder");
    }
}
