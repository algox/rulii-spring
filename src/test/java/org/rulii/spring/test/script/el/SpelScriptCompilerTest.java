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
import org.rulii.script.BuildScriptException;
import org.rulii.script.Script;
import org.rulii.spring.script.el.SpelScript;
import org.rulii.spring.script.el.SpelScriptCompiler;
import org.rulii.spring.script.el.SpelScriptProcessorFactory;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class SpelScriptCompilerTest {

    @Test
    public void testGetLanguageName() {
        SpelScriptCompiler compiler = new SpelScriptCompiler();
        assertEquals(SpelScriptProcessorFactory.LANGUAGE_NAME, compiler.getLanguageName());
        assertEquals("el", compiler.getLanguageName());
    }

    @Test
    public void testCompileReturnsSpelScript() {
        SpelScriptCompiler compiler = new SpelScriptCompiler();
        Script<?> script = compiler.compile("1 + 2", Collections.emptyList());
        assertNotNull(script);
        assertInstanceOf(SpelScript.class, script);
    }

    @Test
    public void testCompilePreservesScriptText() {
        SpelScriptCompiler compiler = new SpelScriptCompiler();
        String expression = "#ctx.value > 10";
        Script<?> script = compiler.compile(expression, Collections.emptyList());
        assertEquals(expression, script.getScript());
    }

    @Test
    public void testCompilePreservesLanguageName() {
        SpelScriptCompiler compiler = new SpelScriptCompiler();
        Script<?> script = compiler.compile("#ctx.a + #ctx.b", Collections.emptyList());
        assertEquals("el", script.getLanguageName());
    }

    @Test
    public void testCompilePopulatesExpression() {
        SpelScriptCompiler compiler = new SpelScriptCompiler();
        Script<?> script = compiler.compile("2 * 3", Collections.emptyList());
        assertInstanceOf(SpelScript.class, script);
        assertNotNull(((SpelScript<?>) script).getExpression());
    }

    @Test
    public void testCompileArithmeticExpression() {
        SpelScriptCompiler compiler = new SpelScriptCompiler();
        Script<?> script = compiler.compile("#ctx.a + #ctx.b", Collections.emptyList());
        assertNotNull(script);
    }

    @Test
    public void testCompileBooleanExpression() {
        SpelScriptCompiler compiler = new SpelScriptCompiler();
        Script<?> script = compiler.compile("#ctx.age >= 18", Collections.emptyList());
        assertNotNull(script);
    }

    @Test
    public void testCompileStringExpression() {
        SpelScriptCompiler compiler = new SpelScriptCompiler();
        Script<?> script = compiler.compile("#ctx.first + ' ' + #ctx.last", Collections.emptyList());
        assertNotNull(script);
    }

    @Test
    public void testCompileInvalidExpressionThrowsBuildScriptException() {
        SpelScriptCompiler compiler = new SpelScriptCompiler();
        assertThrows(BuildScriptException.class, () ->
                compiler.compile("[[[invalid syntax%%%", Collections.emptyList()));
    }

    @Test
    public void testBuildScriptExceptionContainsScript() {
        SpelScriptCompiler compiler = new SpelScriptCompiler();
        String badScript = "[[[invalid";
        BuildScriptException ex = assertThrows(BuildScriptException.class, () ->
                compiler.compile(badScript, Collections.emptyList()));
        assertNotNull(ex);
    }

    @Test
    public void testCompileMultipleTimesProducesIndependentScripts() {
        SpelScriptCompiler compiler = new SpelScriptCompiler();
        Script<?> s1 = compiler.compile("1 + 1", Collections.emptyList());
        Script<?> s2 = compiler.compile("2 + 2", Collections.emptyList());
        assertNotEquals(s1.getScript(), s2.getScript());
    }
}
