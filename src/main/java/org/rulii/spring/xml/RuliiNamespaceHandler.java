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

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class RuliiNamespaceHandler extends NamespaceHandlerSupport {

    /** The rulii namespace URI. */
    static final String NAMESPACE_URI = "http://www.rulii.org/schema/rulii";

    /** Default scripting language used when neither the element nor {@code <scripting>} specifies one. */
    static final String DEFAULT_LANGUAGE = "el";

    /**
     * Resolves the default scripting language for the document containing the given element.
     *
     * <p>The {@code <scripting defaultLanguage="...">} directive is read declaratively: it is
     * scoped to the XML file that declares it (never leaking into other files parsed by the
     * same reader) and applies regardless of its position within the file. When a file
     * contains multiple {@code <scripting>} elements, the first one wins.
     *
     * @param element any element of the document being parsed
     * @return the declared default language, or {@link #DEFAULT_LANGUAGE} if none is declared
     */
    static String getDefaultLanguage(Element element) {
        NodeList list = element.getOwnerDocument().getElementsByTagNameNS(NAMESPACE_URI, "scripting");

        if (list.getLength() > 0) {
            String language = ((Element) list.item(0)).getAttribute("defaultLanguage");
            if (StringUtils.hasText(language)) return language;
        }

        return DEFAULT_LANGUAGE;
    }

    /**
     * Parses an {@code xs:boolean} attribute value. Unlike {@link Boolean#parseBoolean},
     * this honors the full XML Schema lexical space: {@code "true"} and {@code "1"} are
     * true; {@code "false"} and {@code "0"} are false.
     *
     * @param value        the raw attribute value; may be null or blank
     * @param defaultValue the value to return when the attribute is absent or blank
     * @return the parsed boolean
     */
    static boolean parseBooleanAttribute(String value, boolean defaultValue) {
        if (!StringUtils.hasText(value)) return defaultValue;

        String v = value.strip();
        return "true".equals(v) || "1".equals(v);
    }

    @Override
    public void init() {
        registerBeanDefinitionParser("scripting", new ScriptingBeanDefinitionParser());
        registerBeanDefinitionParser("rule", new RuleBeanDefinitionParser());
        registerBeanDefinitionParser("validationRule", new ValidationRuleBeanDefinitionParser());
        registerBeanDefinitionParser("ruleset", new RuleSetBeanDefinitionParser());

        // One shared parser handles every predefined validator element; the element list
        // lives next to the dispatch switch it must stay in sync with.
        PredefinedValidationRuleBeanDefinitionParser predefined = new PredefinedValidationRuleBeanDefinitionParser();
        PredefinedValidationRuleFactoryBean.TYPES.forEach(type -> registerBeanDefinitionParser(type, predefined));
    }
}
