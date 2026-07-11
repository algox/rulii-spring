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

import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * Holds the value source for a predefined validation rule element — either a named binding
 * resolved from the RuleContext at execution time, or a script expression.
 *
 * <p>The value source is parsed directly from the predefined rule element's attributes:
 * <ul>
 *   <li>{@code binding} — shorthand that resolves a named binding from the RuleContext</li>
 *   <li>{@code expr} — script expression provided as an attribute</li>
 *   <li>(text body) — script expression provided as the element's text content</li>
 * </ul>
 *
 * @author Max Arulananthan
 * @since 1.0
 */
class ValueSource {

    private final String bindingName;
    private final ScriptExpression script;

    ValueSource(String bindingName) {
        super();
        Assert.hasText(bindingName, "bindingName cannot be null or empty.");
        this.bindingName = bindingName;
        this.script = null;
    }

    ValueSource(ScriptExpression script) {
        super();
        Assert.notNull(script, "script cannot be null.");
        this.bindingName = null;
        this.script = script;
    }

    boolean isBinding() {
        return bindingName != null;
    }

    String getBindingName() {
        return bindingName;
    }

    ScriptExpression getScript() {
        return script;
    }

    /**
     * Parses a {@link ValueSource} from the given element's attributes.
     * Checks {@code binding} first; falls back to {@code expr} attribute or text body.
     * A missing value source is reported as a parse error with the XML source location.
     *
     * @param element       the predefined validation rule element
     * @param parserContext the parser context used for error reporting
     * @return a {@link ValueSource} representing either a binding or a script expression
     */
    static ValueSource parse(Element element, ParserContext parserContext) {
        String binding = element.getAttribute("binding");
        if (StringUtils.hasText(binding)) return new ValueSource(binding);

        ScriptExpression script = ScriptExpression.tryParse(element);

        if (script == null) {
            parserContext.getReaderContext().error("<" + element.getLocalName()
                    + "> must supply either a binding attribute or a script expression (expr attribute or element body).", element);
            return null;
        }

        return new ValueSource(script);
    }
}
