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

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * Base class for rulii namespace parsers. Holds the single bean-naming policy for the
 * namespace: the element's {@code name} attribute when present, otherwise a generated
 * unique name.
 *
 * @author Max Arulananthan
 * @since 2.0
 */
abstract class AbstractRuliiBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    AbstractRuliiBeanDefinitionParser() {
        super();
    }

    @Override
    protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) {
        return resolveBeanName(element.getAttribute("name"), definition, parserContext);
    }

    /**
     * Returns the given name when non-blank, otherwise generates a unique bean name.
     *
     * @param name          the declared name; may be null or blank
     * @param definition    the bean definition being named
     * @param parserContext the parser context used for name generation
     * @return the resolved bean name
     */
    static String resolveBeanName(String name, AbstractBeanDefinition definition, ParserContext parserContext) {
        return StringUtils.hasText(name) ? name : parserContext.getReaderContext().generateBeanName(definition);
    }
}
