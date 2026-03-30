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
package org.rulii.spring.test.xml;

import org.junit.jupiter.api.Test;
import org.rulii.context.RuleContext;
import org.rulii.rule.Rule;
import org.rulii.rule.RuleExecutionStatus;
import org.rulii.rule.RuleResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the {@code <rulii:rule>} XML namespace element.
 *
 * <p>Loads {@code rule-test.xml} through the {@link org.rulii.spring.xml.RuliiNamespaceHandler}
 * and verifies that each rule bean is created correctly, has the right structural properties,
 * and evaluates its script-based condition as expected.
 *
 * @author Max Arulananthan
 * @since 1.0
 */
@SpringJUnitConfig(locations = "classpath:rules/rule-test.xml")
class RuleXmlTest {

    @Autowired @Qualifier("AgeCheckRule")     private Rule ageCheckRule;
    @Autowired @Qualifier("PositiveValueRule") private Rule positiveValueRule;
    @Autowired @Qualifier("PreConditionRule") private Rule preConditionRule;
    @Autowired @Qualifier("MultiActionRule")  private Rule multiActionRule;
    @Autowired @Qualifier("NoConditionRule")  private Rule noConditionRule;
    @Autowired @Qualifier("LanguageOverrideRule") private Rule languageOverrideRule;

    // ------------------------------------------------------------------
    // AgeCheckRule — inline text, given + then + otherwise
    // ------------------------------------------------------------------

    @Test
    void ageCheckRuleBeanIsCreated() {
        assertNotNull(ageCheckRule);
    }

    @Test
    void ageCheckRuleHasCorrectName() {
        assertEquals("AgeCheckRule", ageCheckRule.getName());
    }

    @Test
    void ageCheckRuleHasCorrectDescription() {
        assertEquals("Checks if a person is an adult", ageCheckRule.getDefinition().getDescription());
    }

    @Test
    void ageCheckRuleIsAnInlineRule() {
        assertTrue(ageCheckRule.getDefinition().isInline(),
                "Script-based rules declared in XML must be inline");
    }

    @Test
    void ageCheckRuleHasGivenCondition() {
        assertNotNull(ageCheckRule.getCondition());
    }

    @Test
    void ageCheckRuleHasThenAction() {
        assertFalse(ageCheckRule.getActions().isEmpty(),
                "Expected at least one then-action");
    }

    @Test
    void ageCheckRuleHasOtherwiseAction() {
        assertNotNull(ageCheckRule.getOtherwiseAction());
    }

    @Test
    void ageCheckRuleConditionPassesForAdult() {
        RuleContext ctx = buildContext();
        ctx.getBindings().bind("age", 25);
        assertTrue(ageCheckRule.isTrue(ctx));
    }

    @Test
    void ageCheckRuleConditionFailsForMinor() {
        RuleContext ctx = buildContext();
        ctx.getBindings().bind("age", 15);
        assertFalse(ageCheckRule.isTrue(ctx));
    }

    @Test
    void ageCheckRuleRunReturnsPassForAdult() {
        RuleContext ctx = buildContext();
        ctx.getBindings().bind("age", 25);
        RuleResult result = ageCheckRule.run(ctx);
        assertTrue(result.status().isPass());
    }

    @Test
    void ageCheckRuleRunReturnsFailForMinor() {
        RuleContext ctx = buildContext();
        ctx.getBindings().bind("age", 15);
        RuleResult result = ageCheckRule.run(ctx);
        assertTrue(result.status().isFail());
    }

    @Test
    void ageCheckRuleBoundaryAtExactly18Passes() {
        RuleContext ctx = buildContext();
        ctx.getBindings().bind("age", 18);
        assertTrue(ageCheckRule.isTrue(ctx));
    }

    @Test
    void ageCheckRuleResultCarriesRuleReference() {
        RuleContext ctx = buildContext();
        ctx.getBindings().bind("age", 25);
        RuleResult result = ageCheckRule.run(ctx);
        assertSame(ageCheckRule, result.rule());
    }

    // ------------------------------------------------------------------
    // PositiveValueRule — expr attribute form, no actions
    // ------------------------------------------------------------------

    @Test
    void positiveValueRuleBeanIsCreated() {
        assertNotNull(positiveValueRule);
    }

    @Test
    void positiveValueRuleHasCorrectName() {
        assertEquals("PositiveValueRule", positiveValueRule.getName());
    }

    @Test
    void positiveValueRuleHasConditionFromExprAttr() {
        assertNotNull(positiveValueRule.getCondition(),
                "Condition set via expr attribute must be present");
    }

    @Test
    void positiveValueRulePassesForPositiveValue() {
        RuleContext ctx = buildContext();
        ctx.getBindings().bind("value", 5);
        assertTrue(positiveValueRule.isTrue(ctx));
    }

    @Test
    void positiveValueRuleFailsForNegativeValue() {
        RuleContext ctx = buildContext();
        ctx.getBindings().bind("value", -3);
        assertFalse(positiveValueRule.isTrue(ctx));
    }

    @Test
    void positiveValueRuleFailsForZero() {
        RuleContext ctx = buildContext();
        ctx.getBindings().bind("value", 0);
        assertFalse(positiveValueRule.isTrue(ctx));
    }

    // ------------------------------------------------------------------
    // PreConditionRule — has a guard that can produce SKIPPED status
    // ------------------------------------------------------------------

    @Test
    void preConditionRuleBeanIsCreated() {
        assertNotNull(preConditionRule);
    }

    @Test
    void preConditionRuleHasPreCondition() {
        assertNotNull(preConditionRule.getPreCondition(),
                "pre-condition element must produce a non-null pre-condition");
    }

    @Test
    void preConditionRuleIsSkippedWhenPreConditionFails() {
        RuleContext ctx = buildContext();
        ctx.getBindings().bind("age", -1);   // pre-condition: age > 0 → false → SKIPPED
        ctx.getBindings().bind("value", 20);
        RuleResult result = preConditionRule.run(ctx);
        assertEquals(RuleExecutionStatus.SKIPPED, result.status());
    }

    @Test
    void preConditionRulePassesWhenBothConditionsMet() {
        RuleContext ctx = buildContext();
        ctx.getBindings().bind("age", 5);    // pre-condition: age > 0 → true
        ctx.getBindings().bind("value", 20); // given: value > 10 → true
        RuleResult result = preConditionRule.run(ctx);
        assertTrue(result.status().isPass());
    }

    @Test
    void preConditionRuleFailsWhenMainConditionFails() {
        RuleContext ctx = buildContext();
        ctx.getBindings().bind("age", 5);    // pre-condition passes
        ctx.getBindings().bind("value", 3);  // given: value > 10 → false
        RuleResult result = preConditionRule.run(ctx);
        assertTrue(result.status().isFail());
    }

    // ------------------------------------------------------------------
    // MultiActionRule — two then-actions
    // ------------------------------------------------------------------

    @Test
    void multiActionRuleBeanIsCreated() {
        assertNotNull(multiActionRule);
    }

    @Test
    void multiActionRuleHasTwoActions() {
        assertEquals(2, multiActionRule.getActions().size());
    }

    @Test
    void multiActionRuleRunsWithoutErrorWhenConditionPasses() {
        RuleContext ctx = buildContext();
        ctx.getBindings().bind("count", 5);
        assertDoesNotThrow(() -> multiActionRule.run(ctx));
    }

    @Test
    void multiActionRulePassesWhenCountPositive() {
        RuleContext ctx = buildContext();
        ctx.getBindings().bind("count", 1);
        RuleResult result = multiActionRule.run(ctx);
        assertTrue(result.status().isPass());
    }

    // ------------------------------------------------------------------
    // NoConditionRule — no <given> element; framework defaults to always-true
    // ------------------------------------------------------------------

    @Test
    void noConditionRuleBeanIsCreated() {
        assertNotNull(noConditionRule);
    }

    @Test
    void noConditionRuleAlwaysPasses() {
        RuleContext ctx = buildContext();
        RuleResult result = noConditionRule.run(ctx);
        assertTrue(result.status().isPass(),
                "Rule with no condition should always pass");
    }

    @Test
    void noConditionRuleHasThenAction() {
        assertFalse(noConditionRule.getActions().isEmpty());
    }

    // ------------------------------------------------------------------
    // LanguageOverrideRule — explicit language="spel" on the given element
    // ------------------------------------------------------------------

    @Test
    void languageOverrideRuleBeanIsCreated() {
        assertNotNull(languageOverrideRule);
    }

    @Test
    void languageOverrideRuleConditionPassesFor42() {
        RuleContext ctx = buildContext();
        ctx.getBindings().bind("value", 42);
        assertTrue(languageOverrideRule.isTrue(ctx));
    }

    @Test
    void languageOverrideRuleConditionFailsForOtherValue() {
        RuleContext ctx = buildContext();
        ctx.getBindings().bind("value", 99);
        assertFalse(languageOverrideRule.isTrue(ctx));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Creates a fresh {@link RuleContext} with standard defaults for each test. */
    private static RuleContext buildContext() {
        return RuleContext.builder().standard().build();
    }
}
