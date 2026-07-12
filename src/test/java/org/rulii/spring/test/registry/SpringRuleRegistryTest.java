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
package org.rulii.spring.test.registry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rulii.model.UnrulyException;
import org.rulii.rule.Rule;
import org.rulii.ruleflow.RuleFlow;
import org.rulii.ruleset.RuleSet;
import org.rulii.spring.registry.SpringRuleRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.event.ContextClosedEvent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SpringRuleRegistry} against real bean factories.
 *
 * <p>Includes regression coverage for: enumeration/lookup hierarchy consistency (rules in
 * parent contexts appear in {@code getRules()}), {@code getRuleFlows()} enumeration, and
 * metadata-based type matching in {@code get(name, type)}.
 *
 * @author Max Arulananthan
 * @since 1.0
 */
public class SpringRuleRegistryTest {

    private DefaultListableBeanFactory beanFactory;
    private SpringRuleRegistry registry;

    private final Rule rule1 = mock(Rule.class);
    private final Rule rule2 = mock(Rule.class);
    @SuppressWarnings("rawtypes")
    private final RuleSet ruleSet1 = mock(RuleSet.class);
    @SuppressWarnings("rawtypes")
    private final RuleFlow ruleFlow1 = mock(RuleFlow.class);

    @BeforeEach
    public void setUp() {
        beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("rule1", rule1);
        beanFactory.registerSingleton("rule2", rule2);
        beanFactory.registerSingleton("ruleSet1", ruleSet1);
        beanFactory.registerSingleton("ruleFlow1", ruleFlow1);
        beanFactory.registerSingleton("notARule", "just a string");
        registry = new SpringRuleRegistry(beanFactory);
    }

    @Test
    public void testIsNameInUse() {
        assertTrue(registry.isNameInUse("rule1"));
        assertFalse(registry.isNameInUse("unknown"));
    }

    @Test
    public void testGetCountCountsAllRunnables() {
        // 2 rules + 1 ruleset + 1 ruleflow; the String bean does not count
        assertEquals(4, registry.getCount());
    }

    @Test
    public void testGetRulesReturnsAllRules() {
        assertEquals(2, registry.getRules().size());
        assertTrue(registry.getRules().contains(rule1));
        assertTrue(registry.getRules().contains(rule2));
    }

    @Test
    public void testGetRuleSetsReturnsAllRuleSets() {
        assertEquals(1, registry.getRuleSets().size());
        assertTrue(registry.getRuleSets().contains(ruleSet1));
    }

    @Test
    public void testGetRuleFlowsReturnsAllRuleFlows() {
        assertEquals(1, registry.getRuleFlows().size());
        assertTrue(registry.getRuleFlows().contains(ruleFlow1));
    }

    @Test
    public void testEnumerationIncludesParentContext() {
        DefaultListableBeanFactory parent = new DefaultListableBeanFactory();
        Rule parentRule = mock(Rule.class);
        parent.registerSingleton("parentRule", parentRule);

        DefaultListableBeanFactory child = new DefaultListableBeanFactory(parent);
        child.registerSingleton("childRule", rule1);

        SpringRuleRegistry hierarchical = new SpringRuleRegistry(child);
        // by-name lookup traverses the hierarchy - enumeration must agree
        assertNotNull(hierarchical.get("parentRule", Rule.class));
        assertEquals(2, hierarchical.getRules().size());
        assertTrue(hierarchical.getRules().contains(parentRule));
    }

    @Test
    public void testGetByNameAndType() {
        assertEquals(rule1, registry.get("rule1", Rule.class));
    }

    @Test
    public void testGetByNameDelegatesToFactory() {
        assertEquals(rule1, registry.get("rule1"));
    }

    @Test
    public void testGetUnknownNameReturnsNull() {
        assertNull(registry.get("unknown"));
        assertNull(registry.get("unknown", Rule.class));
    }

    @Test
    public void testGetRuleUnknownNameReturnsNull() {
        assertNull(registry.getRule("unknown"));
    }

    @Test
    public void testGetRuleSetUnknownNameReturnsNull() {
        assertNull(registry.getRuleSet("unknown"));
    }

    @Test
    public void testGetWrongTypeReturnsNull() {
        // 'ruleSet1' exists but is not a Rule - resolved via metadata, no exception flow
        assertNull(registry.get("ruleSet1", Rule.class));
        // a non-Runnable bean is also null through the Runnable-typed lookup
        assertNull(registry.get("notARule"));
    }

    @Test
    public void testHandleContextClosedEventNullsContext() {
        registry.onContextClosed(mock(ContextClosedEvent.class));
        assertThrows(UnrulyException.class, () -> registry.isNameInUse("anything"));
        assertThrows(UnrulyException.class, registry::getCount);
    }

    @Test
    public void testNullFactoryThrows() {
        assertThrows(Exception.class, () -> new SpringRuleRegistry(null));
    }

    @Test
    public void testToStringContainsClassName() {
        assertTrue(registry.toString().contains("SpringRuleRegistry"));
    }
}
