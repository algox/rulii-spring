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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * Parses a {@code <rulii:scripting>} element and updates the {@link RuliiNamespaceHandler}'s
 * {@code defaultLanguage}. This element does not register a Spring bean; it is purely a
 * namespace-level configuration directive.
 *
 * <p>Must appear before any {@code <rulii:rule>} or {@code <rulii:ruleset>} elements to
 * ensure the default language is applied to all subsequent expression elements.
 *
 * @author Max Arulananthan
 * @since 1.0
 */
class ScriptingBeanDefinitionParser implements BeanDefinitionParser {

    private final RuliiNamespaceHandler handler;

    ScriptingBeanDefinitionParser(RuliiNamespaceHandler handler) {
        this.handler = handler;
    }

    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        String defaultLanguage = element.getAttribute("defaultLanguage");
        if (StringUtils.hasText(defaultLanguage)) {
            handler.setDefaultLanguage(defaultLanguage);
        }
        return null;
    }
}
