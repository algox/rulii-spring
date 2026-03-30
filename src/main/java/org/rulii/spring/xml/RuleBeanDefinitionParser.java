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
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

import java.util.List;

/**
 * Parses a {@code <rulii:rule>} element and registers a {@link RuleFactoryBean} bean
 * definition whose name equals the element's {@code name} attribute.
 *
 * <p>Supported child elements:
 * <ul>
 *   <li>{@code <pre-condition>} — guard evaluated before the main condition</li>
 *   <li>{@code <given>} — main boolean condition</li>
 *   <li>{@code <then>} — one or more side-effect actions (executed in order)</li>
 *   <li>{@code <otherwise>} — action executed when the condition is false</li>
 * </ul>
 *
 * <p>Each child element accepts either an {@code expr} attribute or inline text as the script
 * body, plus an optional {@code language} override.
 *
 * @author Max Arulananthan
 * @since 1.0
 */
class RuleBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    private final RuliiNamespaceHandler handler;

    RuleBeanDefinitionParser(RuliiNamespaceHandler handler) {
        this.handler = handler;
    }

    @Override
    protected Class<?> getBeanClass(Element element) {
        return RuleFactoryBean.class;
    }

    @Override
    protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) {
        String name = element.getAttribute("name");
        if (StringUtils.hasText(name)) return name;
        return parserContext.getReaderContext().generateBeanName(definition);
    }

    @Override
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {

        builder.addPropertyValue("name", element.getAttribute("name"));
        builder.addPropertyValue("description", element.getAttribute("description"));
        builder.addPropertyValue("defaultLanguage", handler.getDefaultLanguage());

        Element preCond = DomUtils.getChildElementByTagName(element, "pre-condition");
        if (preCond != null) {
            builder.addPropertyValue("preCondition", ScriptExpression.parse(preCond));
        }

        Element given = DomUtils.getChildElementByTagName(element, "given");
        if (given != null) {
            builder.addPropertyValue("condition", ScriptExpression.parse(given));
        }

        List<Element> actions = DomUtils.getChildElementsByTagName(element, "then");
        if (!actions.isEmpty()) {
            ManagedList<ScriptExpression> thenActions = new ManagedList<>();
            actions.forEach(e -> thenActions.add(ScriptExpression.parse(e)));
            builder.addPropertyValue("thenActions", thenActions);
        }

        Element otherwise = DomUtils.getChildElementByTagName(element, "otherwise");
        if (otherwise != null) {
            builder.addPropertyValue("otherwiseAction", ScriptExpression.parse(otherwise));
        }
    }
}
