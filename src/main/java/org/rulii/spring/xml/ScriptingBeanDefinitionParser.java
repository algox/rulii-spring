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
import org.w3c.dom.Element;

/**
 * Parser for the {@code <rulii:scripting>} element. The element registers no Spring bean
 * and carries no parse-time behavior: it is a purely declarative, file-scoped directive
 * read by the other parsers via {@link RuliiNamespaceHandler#getDefaultLanguage(Element)}.
 *
 * <p>Because the directive is looked up from the document rather than applied while
 * parsing, it takes effect regardless of its position in the file, and it never affects
 * other XML files parsed by the same reader.
 *
 * @author Max Arulananthan
 * @since 1.0
 */
class ScriptingBeanDefinitionParser implements BeanDefinitionParser {

    ScriptingBeanDefinitionParser() {
        super();
    }

    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        return null;
    }
}
