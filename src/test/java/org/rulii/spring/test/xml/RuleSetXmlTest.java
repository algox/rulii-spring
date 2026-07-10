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
import org.rulii.model.InputParameter;
import org.rulii.ruleset.RuleSet;
import org.rulii.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the {@code <rulii:ruleset>} XML namespace element.
 *
 * <p>Covers:
 * <ul>
 *   <li>Basic ruleset creation and name / description</li>
 *   <li>Rule composition via {@code <rule-ref beanName="...">}</li>
 *   <li>Inline {@code <rule>} elements declared inside {@code <rules>}</li>
 *   <li>Input parameter declarations ({@code <param>}) — name, type, required flag</li>
 *   <li>Lifecycle hooks — pre-condition, initializer, stop-condition, finalizer</li>
 *   <li>End-to-end execution via {@link RuleSet#run(RuleContext)}</li>
 * </ul>
 *
 * @author Max Arulananthan
 * @since 1.0
 */
@SpringJUnitConfig(locations = "classpath:rules/ruleset-test.xml")
class RuleSetXmlTest {

    @Autowired @Qualifier("BasicRuleSet")        private RuleSet<?> basicRuleSet;
    @Autowired @Qualifier("ParameterizedRuleSet") private RuleSet<?> parameterizedRuleSet;
    @Autowired @Qualifier("LifecycleRuleSet")    private RuleSet<?> lifecycleRuleSet;
    @Autowired @Qualifier("ValidatingFinalizerRuleSet") private RuleSet<?> validatingFinalizerRuleSet;

    // Standalone rules also registered as beans in the same XML
    @Autowired @Qualifier("AdultCheckRule")  private Rule adultCheckRule;
    @Autowired @Qualifier("PositiveCountRule") private Rule positiveCountRule;

    // ------------------------------------------------------------------
    // Standalone rule beans declared in ruleset-test.xml
    // ------------------------------------------------------------------

    @Test
    void standaloneAdultCheckRuleBeanIsCreated() {
        assertNotNull(adultCheckRule);
    }

    @Test
    void standaloneAdultCheckRuleHasCorrectName() {
        assertEquals("AdultCheckRule", adultCheckRule.getName());
    }

    @Test
    void standalonePositiveCountRuleBeanIsCreated() {
        assertNotNull(positiveCountRule);
    }

    // ------------------------------------------------------------------
    // BasicRuleSet — two rule-refs
    // ------------------------------------------------------------------

    @Test
    void basicRuleSetBeanIsCreated() {
        assertNotNull(basicRuleSet);
    }

    @Test
    void basicRuleSetHasCorrectName() {
        assertEquals("BasicRuleSet", basicRuleSet.getName());
    }

    @Test
    void basicRuleSetHasCorrectDescription() {
        assertEquals("Wraps two standalone rules", basicRuleSet.getDefinition().getDescription());
    }

    @Test
    void basicRuleSetContainsTwoRules() {
        assertEquals(2, basicRuleSet.getRules().size());
    }

    @Test
    void basicRuleSetHasNoInputParameters() {
        assertTrue(basicRuleSet.getInputParameters().isEmpty());
    }

    @Test
    void basicRuleSetHasNoPreCondition() {
        assertNull(basicRuleSet.getPreCondition());
    }

    @Test
    void basicRuleSetExecutesWithoutException() {
        RuleContext ctx = buildContext();
        ctx.getBindings().bind("age", 25);
        ctx.getBindings().bind("count", 3);
        assertDoesNotThrow(() -> basicRuleSet.run(ctx));
    }

    @Test
    void basicRuleSetFirstRuleIsAdultCheckRule() {
        List<Rule> rules = basicRuleSet.getRules();
        assertEquals("AdultCheckRule", rules.get(0).getName());
    }

    @Test
    void basicRuleSetSecondRuleIsPositiveCountRule() {
        List<Rule> rules = basicRuleSet.getRules();
        assertEquals("PositiveCountRule", rules.get(1).getName());
    }

    // ------------------------------------------------------------------
    // ParameterizedRuleSet — params + inline rule
    // ------------------------------------------------------------------

    @Test
    void parameterizedRuleSetBeanIsCreated() {
        assertNotNull(parameterizedRuleSet);
    }

    @Test
    void parameterizedRuleSetHasCorrectName() {
        assertEquals("ParameterizedRuleSet", parameterizedRuleSet.getName());
    }

    @Test
    void parameterizedRuleSetHasTwoInputParameters() {
        assertEquals(2, parameterizedRuleSet.getInputParameters().size());
    }

    @Test
    void parameterizedRuleSetOrderParameterIsPresent() {
        InputParameter<?> orderParam = findParam(parameterizedRuleSet, "order");
        assertNotNull(orderParam, "Expected 'order' parameter");
    }

    @Test
    void parameterizedRuleSetOrderParameterHasCorrectType() {
        InputParameter<?> orderParam = findParam(parameterizedRuleSet, "order");
        assertNotNull(orderParam);
        assertEquals(String.class, orderParam.type());
    }

    @Test
    void parameterizedRuleSetOrderParameterIsRequired() {
        InputParameter<?> orderParam = findParam(parameterizedRuleSet, "order");
        assertNotNull(orderParam);
        assertTrue(orderParam.required());
    }

    @Test
    void parameterizedRuleSetMaxItemsParameterIsPresent() {
        InputParameter<?> maxItemsParam = findParam(parameterizedRuleSet, "maxItems");
        assertNotNull(maxItemsParam, "Expected 'maxItems' parameter");
    }

    @Test
    void parameterizedRuleSetMaxItemsParameterHasCorrectType() {
        InputParameter<?> maxItemsParam = findParam(parameterizedRuleSet, "maxItems");
        assertNotNull(maxItemsParam);
        assertEquals(Integer.class, maxItemsParam.type());
    }

    @Test
    void parameterizedRuleSetMaxItemsParameterIsOptional() {
        InputParameter<?> maxItemsParam = findParam(parameterizedRuleSet, "maxItems");
        assertNotNull(maxItemsParam);
        assertFalse(maxItemsParam.required());
    }

    @Test
    void parameterizedRuleSetContainsOneInlineRule() {
        assertEquals(1, parameterizedRuleSet.getRules().size());
    }

    @Test
    void parameterizedRuleSetInlineRuleHasCorrectName() {
        assertEquals("OrderNotEmptyRule", parameterizedRuleSet.getRules().get(0).getName());
    }

    @Test
    void parameterizedRuleSetExecutesWithRequiredBindings() {
        RuleContext ctx = buildContext();
        ctx.getBindings().bind("order", "ORD-001");
        assertDoesNotThrow(() -> parameterizedRuleSet.run(ctx));
    }

    // ------------------------------------------------------------------
    // LifecycleRuleSet — pre-condition, initializer, stop-condition, finalizer
    // ------------------------------------------------------------------

    @Test
    void lifecycleRuleSetBeanIsCreated() {
        assertNotNull(lifecycleRuleSet);
    }

    @Test
    void lifecycleRuleSetHasCorrectName() {
        assertEquals("LifecycleRuleSet", lifecycleRuleSet.getName());
    }

    @Test
    void lifecycleRuleSetHasPreCondition() {
        assertNotNull(lifecycleRuleSet.getPreCondition(),
                "<pre-condition> must produce a non-null Condition on the RuleSet");
    }

    @Test
    void lifecycleRuleSetContainsOneInlineRule() {
        assertEquals(1, lifecycleRuleSet.getRules().size());
    }

    @Test
    void lifecycleRuleSetInlineRuleIsCorrectlyNamed() {
        assertEquals("LifecycleInlineRule", lifecycleRuleSet.getRules().get(0).getName());
    }

    @Test
    void lifecycleRuleSetExecutesWhenPreConditionPasses() {
        RuleContext ctx = buildContext();
        ctx.getBindings().bind("value", 10);  // pre-condition: value > 0 → true
        ctx.getBindings().bind("count", 1);   // stop-condition: count > 100 → false (no early stop)
        assertDoesNotThrow(() -> lifecycleRuleSet.run(ctx));
    }

    @Test
    void lifecycleRuleSetExecutesWithInlineRulePassingCondition() {
        RuleContext ctx = buildContext();
        ctx.getBindings().bind("value", 10);  // pre-condition and inline rule: value > 0 and value > 5
        ctx.getBindings().bind("count", 1);
        assertDoesNotThrow(() -> lifecycleRuleSet.run(ctx));
    }

    // ------------------------------------------------------------------
    // ValidatingFinalizerRuleSet — validating="true" + custom finalizer
    // ------------------------------------------------------------------

    @Test
    void validatingFinalizerRuleSetBeanIsCreated() {
        assertNotNull(validatingFinalizerRuleSet);
    }

    @Test
    void validatingRuleSetWithFinalizerThrowsValidationExceptionOnFailure() {
        RuleContext ctx = buildContext();
        ctx.getBindings().bind("value", String.class, null);   // NotNull rule fails
        ctx.getBindings().bind("finalized", false);

        assertThrows(ValidationException.class, () -> validatingFinalizerRuleSet.run(ctx));
        assertTrue(ctx.getBindings().getValue("finalized", Boolean.class),
                "custom finalizer must run even though validating mode threw");
    }

    @Test
    void validatingRuleSetWithFinalizerRunsCleanlyOnSuccess() {
        RuleContext ctx = buildContext();
        ctx.getBindings().bind("value", "present");            // NotNull rule passes
        ctx.getBindings().bind("finalized", false);

        assertDoesNotThrow(() -> validatingFinalizerRuleSet.run(ctx));
        assertTrue(ctx.getBindings().getValue("finalized", Boolean.class));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static RuleContext buildContext() {
        return RuleContext.builder().standard().build();
    }

    private static InputParameter<?> findParam(RuleSet<?> ruleSet, String name) {
        return ruleSet.getInputParameters().stream()
                .filter(p -> name.equals(p.name()))
                .findFirst()
                .orElse(null);
    }
}
