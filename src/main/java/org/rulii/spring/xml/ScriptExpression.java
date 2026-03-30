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
package org.rulii.spring.xml;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * Holds a parsed script expression together with its optional language override, name, and
 * description. Instances are created by the namespace parsers and stored on the FactoryBean
 * property bags; they are resolved to live {@link org.rulii.script.Script} objects during
 * bean initialisation.
 *
 * <p>The expression text may come from either the {@code expr} attribute or the element's
 * text content, whichever is non-blank. The {@link #resolveLanguage(String)} helper falls
 * back to the supplied default when no explicit language is set.
 *
 * @author Max Arulananthan
 * @since 1.0
 */
public class ScriptExpression {

    private final String expression;
    private final String language;

    public ScriptExpression(String language, String expression) {
        super();
        Assert.notNull(expression, "expression cannot be null.");
        this.expression = expression;
        this.language   = language;
    }

    /** Explicit scripting-language name, or {@code null} if the default should be used. */
    public String getLanguage() { return language; }

    /** Raw expression text — may be null if the element carried no body or {@code expr} attribute. */
    public String getExpression() { return expression; }

    /**
     * Returns this expression's language if one was specified, otherwise falls back to
     * {@code defaultLanguage}.
     */
    public String resolveLanguage(String defaultLanguage) {
        return (language != null && !language.isBlank()) ? language : defaultLanguage;
    }

    /** Build a {@link ScriptExpression} from a condition/action child element. */
    static ScriptExpression parse(Element element) {
        String expr = element.getAttribute("expr");

        if (!StringUtils.hasText(expr)) {
            expr = element.getTextContent();
            if (expr != null) expr = expr.strip();
        }

        String language = element.getAttribute("language");
        return new ScriptExpression(
                StringUtils.hasText(language) ? language : null,
                StringUtils.hasText(expr) ? expr : null);
    }

    @Override
    public String toString() {
        return "ScriptExpression{expr='" + expression + "', language='" + language + "'}";
    }
}
