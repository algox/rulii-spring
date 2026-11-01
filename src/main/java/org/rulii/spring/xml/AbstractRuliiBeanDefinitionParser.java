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
        // parseInternal() already set the source; the defining resource is only recorded by
        // Spring's own <bean> parser, so record it here - the one hook that sees the finished
        // definition before it is registered.
        definition.setResource(parserContext.getReaderContext().getResource());
        return resolveBeanName(element.getAttribute("name"), definition, parserContext);
    }

    /**
     * Records where an inline bean definition came from, as top-level ones get it: the
     * defining resource, and the element's {@link XmlSource} when the reader's source
     * extractor provides one (see {@link XmlSourceExtractor}).
     *
     * @param def           the definition being registered
     * @param element       the element that declared it
     * @param parserContext the parser context
     */
    /**
     * The result type declared on a ruleset or ruleflow: the {@code result-type} attribute on the parent, or
     * the {@code type} attribute on its result function element ({@code <result-extractor>}, {@code <returning>}).
     * Declaring both is reported as a parse error with the XML source location.
     *
     * @param parent         the ruleset or ruleflow element.
     * @param resultFunction the result function child element; may be null.
     * @param parentName     the parent's element name, for the error.
     * @param functionName   the result function's element name, for the error.
     * @param parserContext  the parser context used for error reporting.
     * @return the fully-qualified class name, or null when neither form is declared.
     */
    static String resultTypeName(Element parent, Element resultFunction, String parentName, String functionName,
                                 ParserContext parserContext) {
        String onParent = parent.getAttribute("result-type");
        String onFunction = resultFunction != null ? resultFunction.getAttribute("type") : null;

        if (StringUtils.hasText(onParent) && StringUtils.hasText(onFunction)) {
            parserContext.getReaderContext().error("<" + parentName + "> declares the result type both as [result-type]"
                    + " and as [type] on <" + functionName + ">; use one or the other.", parent);
        }

        if (StringUtils.hasText(onParent)) return onParent.trim();
        if (StringUtils.hasText(onFunction)) return onFunction.trim();

        return null;
    }

    static void stampSource(AbstractBeanDefinition def, Element element, ParserContext parserContext) {
        def.setResource(parserContext.getReaderContext().getResource());
        def.setSource(parserContext.extractSource(element));
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
