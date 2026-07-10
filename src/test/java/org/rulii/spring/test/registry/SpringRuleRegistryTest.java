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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rulii.model.UnrulyException;
import org.rulii.rule.Rule;
import org.rulii.ruleset.RuleSet;
import org.rulii.spring.registry.SpringRuleRegistry;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.event.ContextClosedEvent;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SpringRuleRegistryTest {

    @Mock
    private ListableBeanFactory beanFactory;

    @Test
    public void testIsNameInUseReturnsTrue() {
        when(beanFactory.containsBean("existingRule")).thenReturn(true);
        SpringRuleRegistry registry = new SpringRuleRegistry(beanFactory);
        assertTrue(registry.isNameInUse("existingRule"));
    }

    @Test
    public void testIsNameInUseReturnsFalse() {
        when(beanFactory.containsBean("unknown")).thenReturn(false);
        SpringRuleRegistry registry = new SpringRuleRegistry(beanFactory);
        assertFalse(registry.isNameInUse("unknown"));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void testGetCountReturnsBeanCount() {
        Map<String, org.rulii.model.Runnable> runnables = new LinkedHashMap<>();
        runnables.put("r1", mock(org.rulii.model.Runnable.class));
        runnables.put("r2", mock(org.rulii.model.Runnable.class));
        when(beanFactory.getBeansOfType(org.rulii.model.Runnable.class)).thenReturn(runnables);
        SpringRuleRegistry registry = new SpringRuleRegistry(beanFactory);
        assertEquals(2, registry.getCount());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetRulesReturnsAllRules() {
        Rule rule1 = mock(Rule.class);
        Rule rule2 = mock(Rule.class);
        Map<String, Rule> rulesMap = new LinkedHashMap<>();
        rulesMap.put("rule1", rule1);
        rulesMap.put("rule2", rule2);
        when(beanFactory.getBeansOfType(Rule.class)).thenReturn(rulesMap);
        SpringRuleRegistry registry = new SpringRuleRegistry(beanFactory);
        assertEquals(2, registry.getRules().size());
        assertTrue(registry.getRules().contains(rule1));
        assertTrue(registry.getRules().contains(rule2));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void testGetRuleSetsReturnsAllRuleSets() {
        RuleSet rs1 = mock(RuleSet.class);
        Map<String, RuleSet> rsMap = new LinkedHashMap<>();
        rsMap.put("rs1", rs1);
        when(beanFactory.getBeansOfType(RuleSet.class)).thenReturn(rsMap);
        SpringRuleRegistry registry = new SpringRuleRegistry(beanFactory);
        assertEquals(1, registry.getRuleSets().size());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetByNameAndType() {
        Rule rule = mock(Rule.class);
        when(beanFactory.containsBean("myRule")).thenReturn(true);
        when(beanFactory.getBean("myRule", Rule.class)).thenReturn(rule);
        SpringRuleRegistry registry = new SpringRuleRegistry(beanFactory);
        assertEquals(rule, registry.get("myRule", Rule.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetByNameDelegatesToFactory() {
        Rule rule = mock(Rule.class);
        when(beanFactory.containsBean("myRule")).thenReturn(true);
        when(beanFactory.getBean("myRule", org.rulii.model.Runnable.class)).thenReturn(rule);
        SpringRuleRegistry registry = new SpringRuleRegistry(beanFactory);
        assertEquals(rule, registry.get("myRule"));
    }

    @Test
    public void testGetUnknownNameReturnsNull() {
        when(beanFactory.containsBean("unknown")).thenReturn(false);
        SpringRuleRegistry registry = new SpringRuleRegistry(beanFactory);
        assertNull(registry.get("unknown"));
    }

    @Test
    public void testGetUnknownNameAndTypeReturnsNull() {
        when(beanFactory.containsBean("unknown")).thenReturn(false);
        SpringRuleRegistry registry = new SpringRuleRegistry(beanFactory);
        assertNull(registry.get("unknown", Rule.class));
    }

    @Test
    public void testGetRuleUnknownNameReturnsNull() {
        when(beanFactory.containsBean("unknown")).thenReturn(false);
        SpringRuleRegistry registry = new SpringRuleRegistry(beanFactory);
        assertNull(registry.getRule("unknown"));
    }

    @Test
    public void testGetRuleSetUnknownNameReturnsNull() {
        when(beanFactory.containsBean("unknown")).thenReturn(false);
        SpringRuleRegistry registry = new SpringRuleRegistry(beanFactory);
        assertNull(registry.getRuleSet("unknown"));
    }

    @Test
    public void testGetWrongTypeReturnsNull() {
        when(beanFactory.containsBean("myRuleSet")).thenReturn(true);
        when(beanFactory.getBean("myRuleSet", Rule.class))
                .thenThrow(new BeanNotOfRequiredTypeException("myRuleSet", Rule.class, RuleSet.class));
        SpringRuleRegistry registry = new SpringRuleRegistry(beanFactory);
        assertNull(registry.get("myRuleSet", Rule.class));
    }

    @Test
    public void testHandleContextClosedEventNullsContext() {
        SpringRuleRegistry registry = new SpringRuleRegistry(beanFactory);
        ContextClosedEvent event = mock(ContextClosedEvent.class);
        registry.handleContextRefreshEvent(event);
        assertThrows(UnrulyException.class, () -> registry.isNameInUse("anything"));
    }

    @Test
    public void testIsNameInUseAfterContextCloseThrows() {
        SpringRuleRegistry registry = new SpringRuleRegistry(beanFactory);
        ContextClosedEvent event = mock(ContextClosedEvent.class);
        registry.handleContextRefreshEvent(event);
        assertThrows(UnrulyException.class, () -> registry.isNameInUse("testRule"));
    }

    @Test
    public void testGetCountAfterContextCloseThrows() {
        SpringRuleRegistry registry = new SpringRuleRegistry(beanFactory);
        ContextClosedEvent event = mock(ContextClosedEvent.class);
        registry.handleContextRefreshEvent(event);
        assertThrows(UnrulyException.class, registry::getCount);
    }

    @Test
    public void testNullFactoryThrows() {
        assertThrows(Exception.class, () -> new SpringRuleRegistry(null));
    }

    @Test
    public void testToStringContainsClassName() {
        SpringRuleRegistry registry = new SpringRuleRegistry(beanFactory);
        String str = registry.toString();
        assertNotNull(str);
        assertTrue(str.contains("SpringRuleRegistry"));
    }
}
