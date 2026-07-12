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

import org.rulii.model.action.Action;
import org.rulii.model.condition.Condition;
import org.rulii.model.function.Function;
import org.rulii.script.Script;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * Holds a parsed script expression together with its optional language override, name, and
 * description. Instances are created by the namespace parsers and stored on the FactoryBean
 * property bags; they are resolved to live {@link org.rulii.script.Script} objects during
 * bean initialisation.
 *
 * <p>The expression text may come from either the {@code expr} attribute or the element's
 * direct text content, whichever is non-blank. The {@link #resolveLanguage(String)} helper
 * falls back to the supplied default when no explicit language is set.
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

    /** Raw expression text; never null. */
    public String getExpression() { return expression; }

    /**
     * Returns this expression's language if one was specified, otherwise falls back to
     * {@code defaultLanguage}.
     */
    public String resolveLanguage(String defaultLanguage) {
        return (language != null && !language.isBlank()) ? language : defaultLanguage;
    }

    /**
     * Compiles this expression into a boolean {@link Condition}.
     *
     * @param defaultLanguage the language used when this expression declares none
     * @return the compiled condition
     */
    public Condition toCondition(String defaultLanguage) {
        return Condition.builder().build(toScript(defaultLanguage));
    }

    /**
     * Compiles this expression into a side-effect {@link Action}.
     *
     * @param defaultLanguage the language used when this expression declares none
     * @return the compiled action
     */
    public Action toAction(String defaultLanguage) {
        return Action.builder().build(toScript(defaultLanguage));
    }

    /**
     * Compiles this expression into a value-producing {@link Function}.
     *
     * @param defaultLanguage the language used when this expression declares none
     * @return the compiled function
     */
    public Function<?> toFunction(String defaultLanguage) {
        return Function.builder().build(toScript(defaultLanguage));
    }

    private Script<?> toScript(String defaultLanguage) {
        return Script.builder().build(resolveLanguage(defaultLanguage), expression);
    }

    /**
     * Builds a {@link ScriptExpression} from an expression-bearing element, reporting a
     * parse error (with the XML source location) when the element carries no expression.
     *
     * @param element       the condition/action/function element
     * @param parserContext the parser context used for error reporting
     * @return the parsed expression; never null on the fail-fast error reporter
     */
    static ScriptExpression parse(Element element, ParserContext parserContext) {
        ScriptExpression result = tryParse(element);

        if (result == null) {
            parserContext.getReaderContext().error("<" + element.getLocalName()
                    + "> must provide a script expression (expr attribute or element body).", element);
        }

        return result;
    }

    /**
     * Resolves an expression (condition, action, or function) declared either as an
     * attribute on the parent element (terse form, e.g. {@code <rule given="#ctx.age >= 18">})
     * or as a child element of the same name (e.g. {@code <given>...</given>}). Declaring
     * both is reported as a parse error with the XML source location. The attribute form
     * always uses the file's default scripting language — a per-expression
     * {@code language} override requires the element form.
     *
     * @param parent        the element carrying the condition
     * @param name          the attribute/child-element name (e.g. {@code given})
     * @param parserContext the parser context used for error reporting
     * @return the parsed expression, or {@code null} when neither form is declared
     */
    static ScriptExpression fromAttributeOrChild(Element parent, String name, ParserContext parserContext) {
        String attribute = parent.getAttribute(name);
        Element child = DomUtils.getChildElementByTagName(parent, name);

        if (StringUtils.hasText(attribute) && child != null) {
            parserContext.getReaderContext().error("<" + parent.getLocalName() + "> declares [" + name
                    + "] both as an attribute and as a child element; use one or the other.", parent);
        }

        if (StringUtils.hasText(attribute)) return new ScriptExpression(null, attribute);

        return child != null ? parse(child, parserContext) : null;
    }

    /**
     * Builds a {@link ScriptExpression} from an expression-bearing element, or returns
     * {@code null} when the element has neither an {@code expr} attribute nor a non-blank
     * direct text body. Only direct text/CDATA children count as the body — text inside
     * child elements (e.g. {@code <item>}) is never treated as an expression.
     *
     * @param element the element to read the expression from
     * @return the parsed expression, or {@code null} if none is present
     */
    static ScriptExpression tryParse(Element element) {
        String expr = element.getAttribute("expr");

        if (!StringUtils.hasText(expr)) {
            expr = DomUtils.getTextValue(element).strip();
        }

        if (!StringUtils.hasText(expr)) return null;

        String language = element.getAttribute("language");
        return new ScriptExpression(StringUtils.hasText(language) ? language : null, expr);
    }

    @Override
    public String toString() {
        return "ScriptExpression{expr='" + expression + "', language='" + language + "'}";
    }
}
