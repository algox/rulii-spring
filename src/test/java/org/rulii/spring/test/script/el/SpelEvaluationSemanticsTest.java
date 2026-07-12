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
import org.rulii.context.RuleContext;
import org.rulii.script.AbstractScript;
import org.rulii.script.EvaluationException;
import org.rulii.script.Script;
import org.rulii.spring.script.el.SpelScriptCompiler;
import org.rulii.spring.script.el.SpelScriptProcessor;
import org.rulii.spring.script.el.SpelScriptProcessorFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the SpEL evaluation-context semantics of {@link SpelScriptProcessor}.
 *
 * <p>Regression tests: the processor previously REPLACED the default property accessors
 * (breaking property navigation into binding values), never set a root object (breaking
 * the documented bare-name form), let wrong-language scripts escape as raw
 * ClassCastException, and the compiler stamped every script with the static "el" name
 * regardless of the factory's configured language.
 *
 * @author Max Arulananthan
 * @since 2.0
 */
class SpelEvaluationSemanticsTest {

    /** Simple POJO bound into the rule context to exercise reflective property navigation. */
    public static class Customer {
        private final String name;

        public Customer(String name) {
            super();
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private final SpelScriptProcessor processor = new SpelScriptProcessor("el", "ctx");
    private final SpelScriptCompiler compiler = new SpelScriptCompiler();

    private RuleContext ctx() {
        RuleContext ctx = RuleContext.builder().standard().build();
        ctx.getBindings().bind("age", 21);
        ctx.getBindings().bind("customer", new Customer("Max"));
        return ctx;
    }

    @Test
    void propertyNavigationIntoBindingValueWorks() {
        Script<Object> script = compiler.compile("#ctx.customer.name", Object.class);
        assertEquals("Max", processor.evaluate(script, ctx()),
                "reflective property navigation must work alongside the BindingAccessor");
    }

    @Test
    void bareNameAccessResolvesAgainstBindingsRoot() {
        Script<Object> script = compiler.compile("age >= 18", Object.class);
        assertEquals(Boolean.TRUE, processor.evaluate(script, ctx()),
                "bare binding names must resolve against the Bindings root object");
    }

    @Test
    void bareNameNavigationWorks() {
        Script<Object> script = compiler.compile("customer.name", Object.class);
        assertEquals("Max", processor.evaluate(script, ctx()));
    }

    @Test
    void variableStyleAccessStillWorks() {
        Script<Object> script = compiler.compile("#ctx.age >= 18", Object.class);
        assertEquals(Boolean.TRUE, processor.evaluate(script, ctx()));
    }

    @Test
    void declaredReturnTypeIsApplied() {
        // Expression yields a String; the declared Boolean return type must be converted
        // by SpEL instead of heap-polluting and blowing up later at the call site.
        Script<Boolean> script = compiler.compile("'true'", Boolean.class);
        assertEquals(Boolean.TRUE, processor.evaluate(script, ctx()));
    }

    @Test
    void wrongScriptTypeThrowsEvaluationException() {
        Script<Object> foreign = new AbstractScript<>("other", "1 + 1", Object.class) {};

        EvaluationException ex = assertThrows(EvaluationException.class,
                () -> processor.evaluate(foreign, ctx()));
        assertTrue(ex.getMessage().contains("SpelScript"),
                "error should name the expected script type, but was: " + ex.getMessage());
    }

    @Test
    void customLanguageFactoryStampsItsOwnLanguageName() {
        SpelScriptProcessorFactory factory = new SpelScriptProcessorFactory("spel-custom", "ctx");
        Script<Object> script = factory.getScriptCompiler().compile("1 + 1", Object.class);

        assertEquals("spel-custom", script.getLanguageName(),
                "compiled scripts must carry the factory's language so dispatch finds the right processor");
        assertEquals(2, factory.getScriptProcessor().evaluate(script, ctx()));
    }

    @Test
    void factoryReportsSpelAvailable() {
        assertTrue(new SpelScriptProcessorFactory().isAvailable(),
                "spring-expression is on this classpath, so the factory must be available");
    }
}
