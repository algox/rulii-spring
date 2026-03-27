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
import org.rulii.script.EvaluationException;
import org.rulii.script.Script;
import org.rulii.spring.script.el.SpelScriptCompiler;
import org.rulii.spring.script.el.SpelScriptProcessor;

import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class SpelScriptProcessorTest {

    private static final SpelScriptCompiler COMPILER = new SpelScriptCompiler();

    private static Script<?> compile(String expr) {
        return COMPILER.compile(expr, Collections.emptyList());
    }

    private static RuleContext contextWith(Bindings bindings) {
        return RuleContext.builder().standard().bindings(bindings).build();
    }

    @Test
    public void testGetLanguageName() {
        SpelScriptProcessor processor = new SpelScriptProcessor("el", "ctx");
        assertEquals("el", processor.getLanguageName());
    }

    @Test
    public void testGetBindingName() {
        SpelScriptProcessor processor = new SpelScriptProcessor("el", "myCtx");
        assertEquals("myCtx", processor.getBindingName());
    }

    @Test
    public void testEvaluateAddition() {
        Bindings bindings = Bindings.builder().standard();
        bindings.bind("a", 5);
        bindings.bind("b", 3);
        Script<Integer> script = (Script<Integer>) compile("#ctx.a + #ctx.b");
        SpelScriptProcessor processor = new SpelScriptProcessor("el", "ctx");
        Integer result = processor.evaluate(script, contextWith(bindings));
        assertEquals(8, result);
    }

    @Test
    public void testEvaluateBooleanTrue() {
        Bindings bindings = Bindings.builder().standard();
        bindings.bind("age", 25);
        Script<Boolean> script = (Script<Boolean>) compile("#ctx.age > 18");
        SpelScriptProcessor processor = new SpelScriptProcessor("el", "ctx");
        Boolean result = processor.evaluate(script, contextWith(bindings));
        assertTrue(result);
    }

    @Test
    public void testEvaluateBooleanFalse() {
        Bindings bindings = Bindings.builder().standard();
        bindings.bind("age", 10);
        Script<Boolean> script = (Script<Boolean>) compile("#ctx.age > 18");
        SpelScriptProcessor processor = new SpelScriptProcessor("el", "ctx");
        Boolean result = processor.evaluate(script, contextWith(bindings));
        assertFalse(result);
    }

    @Test
    public void testEvaluateStringConcatenation() {
        Bindings bindings = Bindings.builder().standard();
        bindings.bind("first", "John");
        bindings.bind("last", "Doe");
        Script<String> script = (Script<String>) compile("#ctx.first + ' ' + #ctx.last");
        SpelScriptProcessor processor = new SpelScriptProcessor("el", "ctx");
        String result = processor.evaluate(script, contextWith(bindings));
        assertEquals("John Doe", result);
    }

    @Test
    public void testEvaluateSetsNewBinding() {
        Bindings bindings = Bindings.builder().standard();
        bindings.bind("a", 10);
        bindings.bind("b", 20);
        RuleContext ctx = contextWith(bindings);
        Script<Integer> script = (Script<Integer>) compile("#ctx.c = #ctx.a + #ctx.b");
        SpelScriptProcessor processor = new SpelScriptProcessor("el", "ctx");
        Integer result = processor.evaluate(script, ctx);
        assertEquals(30, result);
        assertEquals(30, (Integer) ctx.getBindings().getValue("c"));
    }

    @Test
    public void testEvaluateLiteralExpression() {
        Bindings bindings = Bindings.builder().standard();
        Script<Integer> script = (Script<Integer>) compile("42");
        SpelScriptProcessor processor = new SpelScriptProcessor("el", "ctx");
        Integer result = processor.evaluate(script, contextWith(bindings));
        assertEquals(42, result);
    }

    @Test
    public void testEvaluateComparisonWithDates() {
        Bindings bindings = Bindings.builder().standard();
        LocalDate today = LocalDate.now();
        LocalDate past = today.minusDays(1);
        bindings.bind("fromDate", past);
        bindings.bind("toDate", today);
        Script<Boolean> script = (Script<Boolean>) compile("#ctx.fromDate.isBefore(#ctx.toDate)");
        SpelScriptProcessor processor = new SpelScriptProcessor("el", "ctx");
        assertTrue(processor.evaluate(script, contextWith(bindings)));
    }

    @Test
    public void testEvaluateThrowsEvaluationExceptionOnRuntimeError() {
        Bindings bindings = Bindings.builder().standard();
        bindings.bind("a", 1);
        bindings.bind("b", 0);
        Script<Integer> script = (Script<Integer>) compile("#ctx.a / #ctx.b");
        SpelScriptProcessor processor = new SpelScriptProcessor("el", "ctx");
        assertThrows(EvaluationException.class, () -> processor.evaluate(script, contextWith(bindings)));
    }

    @Test
    public void testNullLanguageNameThrows() {
        assertThrows(Exception.class, () -> new SpelScriptProcessor(null, "ctx"));
    }

    @Test
    public void testEmptyLanguageNameThrows() {
        assertThrows(Exception.class, () -> new SpelScriptProcessor("", "ctx"));
    }

    @Test
    public void testNullBindingNameThrows() {
        assertThrows(Exception.class, () -> new SpelScriptProcessor("el", null));
    }

    @Test
    public void testEmptyBindingNameThrows() {
        assertThrows(Exception.class, () -> new SpelScriptProcessor("el", ""));
    }
}
