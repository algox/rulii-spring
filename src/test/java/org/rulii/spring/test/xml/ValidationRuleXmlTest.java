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
import org.rulii.rule.RuleResult;
import org.rulii.validation.RuleViolations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the {@code <rulii:validationRule>} XML namespace element.
 *
 * <p>Verifies that validation rules are created with the correct name and condition,
 * and that the condition evaluates correctly given various runtime bindings.
 *
 * @author Max Arulananthan
 * @since 1.0
 */
@SpringJUnitConfig(locations = "classpath:rules/validation-rule-test.xml")
class ValidationRuleXmlTest {

    @Autowired @Qualifier("MinAgeRule")       private Rule minAgeRule;
    @Autowired @Qualifier("RequiredNameRule") private Rule requiredNameRule;
    @Autowired @Qualifier("MaxAgeRule")       private Rule maxAgeRule;

    // ------------------------------------------------------------------
    // MinAgeRule — age >= 0, ERROR severity
    // ------------------------------------------------------------------

    @Test
    void minAgeRuleBeanIsCreated() {
        assertNotNull(minAgeRule);
    }

    @Test
    void minAgeRuleHasCorrectName() {
        assertEquals("MinAgeRule", minAgeRule.getName());
    }

    @Test
    void minAgeRuleIsNotInlineRule() {
        // Validation rules are class-based (SuppliedValidationRule), not inline scripts
        assertFalse(minAgeRule.getDefinition().isInline());
    }

    @Test
    void minAgeRuleHasCondition() {
        assertNotNull(minAgeRule.getCondition());
    }

    @Test
    void minAgeRuleConditionPassesForZeroAge() {
        RuleContext ctx = buildContext();
        ctx.getBindings().bind("age", 0);
        assertTrue(minAgeRule.isTrue(ctx), "age == 0 should satisfy age >= 0");
    }

    @Test
    void minAgeRuleConditionPassesForPositiveAge() {
        RuleContext ctx = buildContext();
        ctx.getBindings().bind("age", 25);
        assertTrue(minAgeRule.isTrue(ctx));
    }

    @Test
    void minAgeRuleConditionFailsForNegativeAge() {
        RuleContext ctx = buildContext();
        ctx.getBindings().bind("age", -1);
        assertFalse(minAgeRule.isTrue(ctx));
    }

    @Test
    void minAgeRuleRunReturnsPassForValidAge() {
        RuleContext ctx = buildContext();
        ctx.getBindings().bind("age", 30);
        RuleResult result = minAgeRule.run(ctx);
        assertTrue(result.status().isPass());
    }

    @Test
    void minAgeRuleRunReturnsFailForInvalidAge() {
        RuleContext ctx = buildContext();
        ctx.getBindings().bind("age", -5);
        ctx.getBindings().bind("violations", new RuleViolations());
        RuleResult result = minAgeRule.run(ctx);
        assertTrue(result.status().isFail());
    }

    // ------------------------------------------------------------------
    // RequiredNameRule — name != null, FATAL severity
    // ------------------------------------------------------------------

    @Test
    void requiredNameRuleBeanIsCreated() {
        assertNotNull(requiredNameRule);
    }

    @Test
    void requiredNameRuleHasCorrectName() {
        assertEquals("RequiredNameRule", requiredNameRule.getName());
    }

    @Test
    void requiredNameRuleHasCondition() {
        assertNotNull(requiredNameRule.getCondition());
    }

    @Test
    void requiredNameRuleConditionPassesWhenNameBound() {
        RuleContext ctx = buildContext();
        ctx.getBindings().bind("name", "Alice");
        assertTrue(requiredNameRule.isTrue(ctx));
    }

    @Test
    void requiredNameRuleConditionFailsWhenNameIsAbsent() {
        // name is not bound → BindingAccessor returns null → null != null → false
        RuleContext ctx = buildContext();
        assertFalse(requiredNameRule.isTrue(ctx));
    }

    @Test
    void requiredNameRuleRunReturnsPassWhenNameProvided() {
        RuleContext ctx = buildContext();
        ctx.getBindings().bind("name", "Bob");
        RuleResult result = requiredNameRule.run(ctx);
        assertTrue(result.status().isPass());
    }

    @Test
    void requiredNameRuleRunReturnsFailWhenNameMissing() {
        RuleContext ctx = buildContext();
        ctx.getBindings().bind("violations", new RuleViolations());
        RuleResult result = requiredNameRule.run(ctx);
        assertTrue(result.status().isFail());
    }

    // ------------------------------------------------------------------
    // MaxAgeRule — age <= 150 via expr attribute, WARNING severity
    // ------------------------------------------------------------------

    @Test
    void maxAgeRuleBeanIsCreated() {
        assertNotNull(maxAgeRule);
    }

    @Test
    void maxAgeRuleHasCorrectName() {
        assertEquals("MaxAgeRule", maxAgeRule.getName());
    }

    @Test
    void maxAgeRuleConditionPassesForNormalAge() {
        RuleContext ctx = buildContext();
        ctx.getBindings().bind("age", 65);
        assertTrue(maxAgeRule.isTrue(ctx));
    }

    @Test
    void maxAgeRuleConditionPassesAtExactBoundary() {
        RuleContext ctx = buildContext();
        ctx.getBindings().bind("age", 150);
        assertTrue(maxAgeRule.isTrue(ctx), "age == 150 should satisfy age <= 150");
    }

    @Test
    void maxAgeRuleConditionFailsWhenAgeExceedsMax() {
        RuleContext ctx = buildContext();
        ctx.getBindings().bind("age", 151);
        assertFalse(maxAgeRule.isTrue(ctx));
    }

    @Test
    void maxAgeRuleRunReturnsPassForValidAge() {
        RuleContext ctx = buildContext();
        ctx.getBindings().bind("age", 100);
        assertTrue(maxAgeRule.run(ctx).status().isPass());
    }

    @Test
    void maxAgeRuleRunReturnsFailForExcessiveAge() {
        RuleContext ctx = buildContext();
        ctx.getBindings().bind("age", 200);
        ctx.getBindings().bind("violations", new RuleViolations());
        assertTrue(maxAgeRule.run(ctx).status().isFail());
    }

    // ------------------------------------------------------------------
    // Cross-rule: both age constraints applied together
    // ------------------------------------------------------------------

    @Test
    void validAgePassesBothMinAndMaxRules() {
        RuleContext ctx = buildContext();
        ctx.getBindings().bind("age", 30);
        assertTrue(minAgeRule.isTrue(ctx), "minAgeRule should pass for age=30");
        assertTrue(maxAgeRule.isTrue(ctx), "maxAgeRule should pass for age=30");
    }

    @Test
    void negativeAgeFailsMinRuleButPassesMaxRule() {
        RuleContext ctx = buildContext();
        ctx.getBindings().bind("age", -1);
        assertFalse(minAgeRule.isTrue(ctx), "minAgeRule should fail for negative age");
        assertTrue(maxAgeRule.isTrue(ctx), "maxAgeRule should pass for age=-1 (< 150)");
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static RuleContext buildContext() {
        return RuleContext.builder().standard().build();
    }
}
