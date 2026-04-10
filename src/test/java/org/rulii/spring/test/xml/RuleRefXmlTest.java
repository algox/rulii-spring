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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.rulii.context.RuleContext;
import org.rulii.ruleset.RuleSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@code <bean-ref>} and {@code <class-ref>} XML namespace elements.
 *
 * <p>Covers:
 * <ul>
 *   <li>{@code <bean-ref name="..."/>} — delegates to an existing Spring-managed Rule bean</li>
 *   <li>{@code <class-ref class="...">} with typed constructor args ({@code value} + {@code type})</li>
 *   <li>{@code <class-ref class="...">} with a plain String constructor arg (no {@code type})</li>
 *   <li>{@code <class-ref class="...">} with a constructor arg supplied as a bean reference ({@code ref})</li>
 *   <li>{@code <class-ref class="...">} with property injection ({@code value} + {@code type})</li>
 *   <li>{@code <class-ref class="...">} with property injection via bean reference ({@code ref})</li>
 *   <li>Mixed: multiple {@code <class-ref>} entries in a single ruleset</li>
 * </ul>
 *
 * @author Max Arulananthan
 * @since 1.0
 */
@SpringJUnitConfig(locations = "classpath:rules/rule-ref-test.xml")
class RuleRefXmlTest {

    @Autowired @Qualifier("BeanRefRuleSet")    private RuleSet<?> beanRefRuleSet;
    @Autowired @Qualifier("RangeRuleSet")      private RuleSet<?> rangeRuleSet;
    @Autowired @Qualifier("PrefixRuleSet")     private RuleSet<?> prefixRuleSet;
    @Autowired @Qualifier("PrefixRefRuleSet")  private RuleSet<?> prefixRefRuleSet;
    @Autowired @Qualifier("ThresholdRuleSet")  private RuleSet<?> thresholdRuleSet;
    @Autowired @Qualifier("MixedRuleSet")      private RuleSet<?> mixedRuleSet;
    @Autowired @Qualifier("PropertyRefRuleSet") private RuleSet<?> propertyRefRuleSet;


    // ══════════════════════════════════════════════════════════════════════
    // <bean-ref> — delegates to an existing Spring-managed Rule bean
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    class BeanRefTests {

        @Test
        void ruleSetIsCreated() {
            assertNotNull(beanRefRuleSet);
        }

        @Test
        void containsOneRule() {
            assertEquals(1, beanRefRuleSet.getRules().size());
        }

        @Test
        void passesWhenAgeIsAtLeast18() {
            assertDoesNotThrow(() -> beanRefRuleSet.run(ctx("age", 18)));
        }

        @Test
        void passesWhenAgeIsAbove18() {
            assertDoesNotThrow(() -> beanRefRuleSet.run(ctx("age", 30)));
        }

        @Test
        void runsWithoutErrorForAgeBelowThreshold() {
            // The AdultRule inline rule has no @Otherwise — it just fails silently (no ValidationException)
            assertDoesNotThrow(() -> beanRefRuleSet.run(ctx("age", 10)));
        }
    }


    // ══════════════════════════════════════════════════════════════════════
    // <class-ref> with typed int constructor args
    // RangeCheckRule(int min=10, int max=100)
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    class TypedConstructorArgTests {

        @Test
        void ruleSetIsCreated() {
            assertNotNull(rangeRuleSet);
        }

        @Test
        void containsOneRule() {
            assertEquals(1, rangeRuleSet.getRules().size());
        }

        @Test
        void passesForValueAtLowerBound() {
            assertDoesNotThrow(() -> rangeRuleSet.run(ctx("value", 10)));
        }

        @Test
        void passesForValueAtUpperBound() {
            assertDoesNotThrow(() -> rangeRuleSet.run(ctx("value", 100)));
        }

        @Test
        void passesForValueWithinRange() {
            assertDoesNotThrow(() -> rangeRuleSet.run(ctx("value", 55)));
        }

        @Test
        void failsForValueBelowRange() {
            assertFalse(rangeRuleSet.getRules().get(0).isTrue(ctx("value", 9)));
        }

        @Test
        void failsForValueAboveRange() {
            assertFalse(rangeRuleSet.getRules().get(0).isTrue(ctx("value", 101)));
        }
    }


    // ══════════════════════════════════════════════════════════════════════
    // <class-ref> with plain String constructor arg (no type attribute)
    // PrefixRule(String prefix="Hello")
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    class PlainStringConstructorArgTests {

        @Test
        void ruleSetIsCreated() {
            assertNotNull(prefixRuleSet);
        }

        @Test
        void containsOneRule() {
            assertEquals(1, prefixRuleSet.getRules().size());
        }

        @Test
        void passesWhenTextStartsWithPrefix() {
            assertTrue(prefixRuleSet.getRules().get(0).isTrue(ctx("text", "Hello World")));
        }

        @Test
        void passesWhenTextExactlyEqualsPrefix() {
            assertTrue(prefixRuleSet.getRules().get(0).isTrue(ctx("text", "Hello")));
        }

        @Test
        void failsWhenTextDoesNotStartWithPrefix() {
            assertFalse(prefixRuleSet.getRules().get(0).isTrue(ctx("text", "Goodbye")));
        }

        @Test
        void failsForEmptyText() {
            assertFalse(prefixRuleSet.getRules().get(0).isTrue(ctx("text", "")));
        }
    }


    // ══════════════════════════════════════════════════════════════════════
    // <class-ref> with constructor arg supplied as a bean reference
    // PrefixRule(String prefix) — prefix wired from the "helloPrefix" bean
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    class BeanRefConstructorArgTests {

        @Test
        void ruleSetIsCreated() {
            assertNotNull(prefixRefRuleSet);
        }

        @Test
        void containsOneRule() {
            assertEquals(1, prefixRefRuleSet.getRules().size());
        }

        @Test
        void passesWhenTextStartsWithPrefixFromBean() {
            assertTrue(prefixRefRuleSet.getRules().get(0).isTrue(ctx("text", "Hello there")));
        }

        @Test
        void failsWhenTextDoesNotStartWithPrefixFromBean() {
            assertFalse(prefixRefRuleSet.getRules().get(0).isTrue(ctx("text", "Hi there")));
        }

        @Test
        void prefixMatchesBeanRefRuleSet() {
            // Both PrefixRuleSet (literal arg) and PrefixRefRuleSet (ref arg) use "Hello" as
            // the prefix — they should agree on every input.
            assertTrue(prefixRuleSet.getRules().get(0).isTrue(ctx("text", "Hello Spring")) ==
                       prefixRefRuleSet.getRules().get(0).isTrue(ctx("text", "Hello Spring")));
            assertTrue(prefixRuleSet.getRules().get(0).isTrue(ctx("text", "Nope")) ==
                       prefixRefRuleSet.getRules().get(0).isTrue(ctx("text", "Nope")));
        }
    }


    // ══════════════════════════════════════════════════════════════════════
    // <class-ref> with property injection (value + type)
    // ThresholdRule — threshold=50 via <property>
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    class PropertyInjectionTests {

        @Test
        void ruleSetIsCreated() {
            assertNotNull(thresholdRuleSet);
        }

        @Test
        void containsOneRule() {
            assertEquals(1, thresholdRuleSet.getRules().size());
        }

        @Test
        void passesForValueAboveThreshold() {
            assertTrue(thresholdRuleSet.getRules().get(0).isTrue(ctx("value", 51)));
        }

        @Test
        void passesForValueWellAboveThreshold() {
            assertTrue(thresholdRuleSet.getRules().get(0).isTrue(ctx("value", 1000)));
        }

        @Test
        void failsForValueEqualToThreshold() {
            assertFalse(thresholdRuleSet.getRules().get(0).isTrue(ctx("value", 50)));
        }

        @Test
        void failsForValueBelowThreshold() {
            assertFalse(thresholdRuleSet.getRules().get(0).isTrue(ctx("value", 0)));
        }
    }


    // ══════════════════════════════════════════════════════════════════════
    // Mixed ruleset — multiple <class-ref> entries with different wiring styles
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    class MixedRuleSetTests {

        @Test
        void ruleSetIsCreated() {
            assertNotNull(mixedRuleSet);
        }

        @Test
        void containsTwoRules() {
            assertEquals(2, mixedRuleSet.getRules().size());
        }

        @Test
        void rangeRulePassesForValueInRange() {
            assertTrue(mixedRuleSet.getRules().get(0).isTrue(ctx("value", 500)));
        }

        @Test
        void rangeRuleFailsForValueOutOfRange() {
            assertFalse(mixedRuleSet.getRules().get(0).isTrue(ctx("value", 1000)));
        }

        @Test
        void thresholdRulePassesForPositiveValue() {
            assertTrue(mixedRuleSet.getRules().get(1).isTrue(ctx("value", 1)));
        }

        @Test
        void thresholdRuleFailsForZero() {
            assertFalse(mixedRuleSet.getRules().get(1).isTrue(ctx("value", 0)));
        }
    }


    // ══════════════════════════════════════════════════════════════════════
    // <class-ref> with property injection via bean reference
    // ThresholdRule — threshold wired from the "thresholdValue" bean (Integer 25)
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    class PropertyRefInjectionTests {

        @Test
        void ruleSetIsCreated() {
            assertNotNull(propertyRefRuleSet);
        }

        @Test
        void containsOneRule() {
            assertEquals(1, propertyRefRuleSet.getRules().size());
        }

        @Test
        void passesForValueAboveThreshold() {
            assertTrue(propertyRefRuleSet.getRules().get(0).isTrue(ctx("value", 26)));
        }

        @Test
        void failsForValueEqualToThreshold() {
            assertFalse(propertyRefRuleSet.getRules().get(0).isTrue(ctx("value", 25)));
        }

        @Test
        void failsForValueBelowThreshold() {
            assertFalse(propertyRefRuleSet.getRules().get(0).isTrue(ctx("value", 10)));
        }
    }


    // ══════════════════════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════════════════════

    private static RuleContext ctx(String name, Object value) {
        RuleContext ctx = RuleContext.builder().standard().build();
        ctx.getBindings().bind(name, value);
        return ctx;
    }
}
