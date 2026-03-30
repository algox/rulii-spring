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
import org.rulii.spring.xml.ScriptExpression;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ScriptExpression} — no Spring context required.
 *
 * <p>Constructor signature: {@code ScriptExpression(String language, String expression)}
 * where {@code language} is optional (nullable) and {@code expression} must not be null.
 *
 * @author Max Arulananthan
 * @since 1.0
 */
class ScriptExpressionTest {

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    @Test
    void constructorAcceptsValidExpressionWithLanguage() {
        ScriptExpression expr = new ScriptExpression("spel", "age >= 18");
        assertEquals("spel",      expr.getLanguage());
        assertEquals("age >= 18", expr.getExpression());
    }

    @Test
    void constructorAcceptsNullLanguage() {
        ScriptExpression expr = new ScriptExpression(null, "age >= 18");
        assertNull(expr.getLanguage());
        assertEquals("age >= 18", expr.getExpression());
    }

    @Test
    void constructorRejectsNullExpression() {
        assertThrows(Exception.class, () -> new ScriptExpression("spel", null),
                "Null expression should be rejected by Assert.notNull");
    }

    @Test
    void constructorAcceptsBlankLanguage() {
        // Blank language is allowed at construction time; resolveLanguage falls back to default
        ScriptExpression expr = new ScriptExpression("  ", "x > 0");
        assertEquals("  ", expr.getLanguage());
    }

    // -----------------------------------------------------------------------
    // getExpression / getLanguage
    // -----------------------------------------------------------------------

    @Test
    void getExpressionReturnsConstructorValue() {
        assertEquals("value > 0", new ScriptExpression("spel", "value > 0").getExpression());
    }

    @Test
    void getLanguageReturnsConstructorValue() {
        assertEquals("groovy", new ScriptExpression("groovy", "value > 0").getLanguage());
    }

    @Test
    void getLanguageReturnsNullWhenNotProvided() {
        assertNull(new ScriptExpression(null, "value > 0").getLanguage());
    }

    // -----------------------------------------------------------------------
    // resolveLanguage
    // -----------------------------------------------------------------------

    @Test
    void resolveLanguageReturnsOwnLanguageWhenSet() {
        ScriptExpression expr = new ScriptExpression("groovy", "x > 0");
        assertEquals("groovy", expr.resolveLanguage("spel"));
    }

    @Test
    void resolveLanguageFallsBackToDefaultWhenLanguageIsNull() {
        ScriptExpression expr = new ScriptExpression(null, "x > 0");
        assertEquals("el", expr.resolveLanguage("el"));
    }

    @Test
    void resolveLanguageFallsBackToDefaultWhenLanguageIsBlank() {
        ScriptExpression expr = new ScriptExpression("  ", "x > 0");
        assertEquals("el", expr.resolveLanguage("el"));
    }

    @Test
    void resolveLanguageUsesAlternativeDefault() {
        ScriptExpression expr = new ScriptExpression(null, "x > 0");
        assertEquals("groovy", expr.resolveLanguage("groovy"));
    }

    @Test
    void resolveLanguageExplicitOverrideTakesPrecedenceOverAnyDefault() {
        ScriptExpression expr = new ScriptExpression("js", "x > 0");
        assertEquals("js", expr.resolveLanguage("el"));
        assertEquals("js", expr.resolveLanguage("groovy"));
        assertEquals("js", expr.resolveLanguage("mvel"));
    }

    // -----------------------------------------------------------------------
    // toString
    // -----------------------------------------------------------------------

    @Test
    void toStringContainsExpression() {
        ScriptExpression expr = new ScriptExpression("spel", "age >= 18");
        assertTrue(expr.toString().contains("age >= 18"));
    }

    @Test
    void toStringContainsLanguage() {
        ScriptExpression expr = new ScriptExpression("spel", "age >= 18");
        assertTrue(expr.toString().contains("spel"));
    }
}
