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
package org.rulii.spring.test.script.el;

import org.junit.jupiter.api.Test;
import org.rulii.bind.Bindings;
import org.rulii.context.RuleContext;
import org.rulii.script.Script;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the SpEL scripting pipeline using the SPI-registered
 * SpelScriptProcessorFactory. Tests run scripts end-to-end via Script.builder().build().
 */
public class SpelScriptIntegrationTest {

    @Test
    public void testSimpleAddition() {
        Script<Integer> script = Script.builder().build("el", "#ctx.c = #ctx.a + #ctx.b");
        RuleContext ctx = RuleContext.builder().standard()
                .bindings(Bindings.builder().standard(a -> 10, b -> 20))
                .build();
        int result = script.run(ctx);
        assertEquals(30, result);
        assertEquals(30, (Integer) ctx.getBindings().getValue("c"));
    }

    @Test
    public void testSimpleSubtraction() {
        Script<Integer> script = Script.builder().build("el", "#ctx.a - #ctx.b");
        Bindings bindings = Bindings.builder().standard();
        bindings.bind("a", 50);
        bindings.bind("b", 20);
        RuleContext ctx = RuleContext.builder().standard().bindings(bindings).build();
        int result = script.run(ctx);
        assertEquals(30, result);
    }

    @Test
    public void testBooleanConditionTrue() {
        Script<Boolean> script = Script.builder().build("el", "#ctx.age >= 18");
        Bindings bindings = Bindings.builder().standard();
        bindings.bind("age", 21);
        RuleContext ctx = RuleContext.builder().standard().bindings(bindings).build();
        assertTrue(script.run(ctx));
    }

    @Test
    public void testBooleanConditionFalse() {
        Script<Boolean> script = Script.builder().build("el", "#ctx.age >= 18");
        Bindings bindings = Bindings.builder().standard();
        bindings.bind("age", 15);
        RuleContext ctx = RuleContext.builder().standard().bindings(bindings).build();
        assertFalse(script.run(ctx));
    }

    @Test
    public void testStringConcatenation() {
        Script<String> script = Script.builder().build("el", "#ctx.firstName + ' ' + #ctx.lastName");
        Bindings bindings = Bindings.builder().standard();
        bindings.bind("firstName", "John");
        bindings.bind("lastName", "Doe");
        RuleContext ctx = RuleContext.builder().standard().bindings(bindings).build();
        assertEquals("John Doe", script.run(ctx));
    }

    @Test
    public void testDateComparison() {
        Script<Boolean> script = Script.builder().build("el", "#ctx.fromDate.isBefore(#ctx.toDate)");
        Bindings bindings = Bindings.builder().standard();
        bindings.bind("fromDate", LocalDate.of(2020, 1, 1));
        bindings.bind("toDate", LocalDate.of(2025, 1, 1));
        RuleContext ctx = RuleContext.builder().standard().bindings(bindings).build();
        assertTrue(script.run(ctx));
    }

    @Test
    public void testLiteralIntegerExpression() {
        Script<Integer> script = Script.builder().build("el", "100");
        RuleContext ctx = RuleContext.builder().standard().build();
        assertEquals(100, (int) script.run(ctx));
    }

    @Test
    public void testLiteralStringExpression() {
        Script<String> script = Script.builder().build("el", "'hello world'");
        RuleContext ctx = RuleContext.builder().standard().build();
        assertEquals("hello world", script.run(ctx));
    }

    @Test
    public void testNullCoalescingWithTernary() {
        Script<String> script = Script.builder().build("el", "#ctx.name != null ? #ctx.name : 'default'");
        Bindings bindings = Bindings.builder().standard();
        bindings.bind("name", (String) null);
        RuleContext ctx = RuleContext.builder().standard().bindings(bindings).build();
        assertEquals("default", script.run(ctx));
    }

    @Test
    public void testMultipleBindingUpdates() {
        Bindings bindings = Bindings.builder().standard();
        bindings.bind("x", 5);
        bindings.bind("y", 10);
        RuleContext ctx = RuleContext.builder().standard().bindings(bindings).build();

        Script<Integer> sum = Script.builder().build("el", "#ctx.sum = #ctx.x + #ctx.y");
        sum.run(ctx);
        assertEquals(15, (Integer) ctx.getBindings().getValue("sum"));

        Script<Integer> product = Script.builder().build("el", "#ctx.product = #ctx.x * #ctx.y");
        product.run(ctx);
        assertEquals(50, (Integer) ctx.getBindings().getValue("product"));
    }

    @Test
    public void testCollectionSize() {
        Script<Integer> script = Script.builder().build("el", "#ctx.items.size()");
        Bindings bindings = Bindings.builder().standard();
        bindings.bind("items", List.of("a", "b", "c"));
        RuleContext ctx = RuleContext.builder().standard().bindings(bindings).build();
        assertEquals(3, (int) script.run(ctx));
    }

    @Test
    public void testStringMethod() {
        Script<String> script = Script.builder().build("el", "#ctx.text.toUpperCase()");
        Bindings bindings = Bindings.builder().standard();
        bindings.bind("text", "hello");
        RuleContext ctx = RuleContext.builder().standard().bindings(bindings).build();
        assertEquals("HELLO", script.run(ctx));
    }

    @Test
    public void testComplexArithmeticExpression() {
        Script<Integer> script = Script.builder().build("el", "(#ctx.a + #ctx.b) * #ctx.c");
        Bindings bindings = Bindings.builder().standard();
        bindings.bind("a", 3);
        bindings.bind("b", 7);
        bindings.bind("c", 4);
        RuleContext ctx = RuleContext.builder().standard().bindings(bindings).build();
        assertEquals(40, (int) script.run(ctx));
    }
}
