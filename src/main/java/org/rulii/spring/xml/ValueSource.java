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
        this.bindingName = bindingName;
        this.script = null;
    }

    ValueSource(ScriptExpression script) {
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
     *
     * @param element the predefined validation rule element
     * @return a {@link ValueSource} representing either a binding or a script expression
     */
    static ValueSource parse(Element element) {
        String binding = element.getAttribute("binding");
        if (StringUtils.hasText(binding)) return new ValueSource(binding);
        return new ValueSource(ScriptExpression.parse(element));
    }
}
