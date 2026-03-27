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
import org.rulii.spring.script.el.SpelScript;
import org.rulii.spring.script.el.SpelScriptCompiler;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class SpelScriptTest {

    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final SpelScriptCompiler compiler = new SpelScriptCompiler();

    @Test
    public void testGetLanguageName() {
        SpelScript<?> script = (SpelScript<?>) compiler.compile("1 + 1", Collections.emptyList());
        assertEquals("el", script.getLanguageName());
    }

    @Test
    public void testGetScript() {
        String expr = "#ctx.value * 2";
        SpelScript<?> script = (SpelScript<?>) compiler.compile(expr, Collections.emptyList());
        assertEquals(expr, script.getScript());
    }

    @Test
    public void testGetExpressionIsNotNull() {
        SpelScript<?> script = (SpelScript<?>) compiler.compile("true", Collections.emptyList());
        assertNotNull(script.getExpression());
    }

    @Test
    public void testGetExpressionMatchesInput() {
        Expression expression = parser.parseExpression("1 + 2");
        SpelScript<?> script = new SpelScript<>("el", "1 + 2", Collections.emptyList(), expression);
        assertEquals(expression, script.getExpression());
    }

    @Test
    public void testNullExpressionThrows() {
        assertThrows(Exception.class, () ->
                new SpelScript<>("el", "some expr", Collections.emptyList(), null));
    }

    @Test
    public void testToString() {
        SpelScript<?> script = (SpelScript<?>) compiler.compile("42", Collections.emptyList());
        String str = script.toString();
        assertNotNull(str);
        assertTrue(str.contains("SpelScript"));
    }
}
