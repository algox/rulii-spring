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
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

import java.util.List;

/**
 * Parses all predefined validation rule elements (e.g. {@code <rulii:notNull>},
 * {@code <rulii:min>}, {@code <rulii:pattern>}) and registers a
 * {@link PredefinedValidationRuleFactoryBean} bean definition for each.
 *
 * <p>The element's local name is stored as the {@code type} property and drives dispatch
 * to the appropriate {@link org.rulii.validation.rules.Validators} factory method at startup.
 *
 * <p>Common attributes parsed from every element:
 * <ul>
 *   <li>{@code name} (required) — bean name</li>
 *   <li>{@code binding} — shorthand value source: a named binding from the RuleContext</li>
 *   <li>{@code expr} — script expression value source (alternative to binding)</li>
 *   <li>{@code language} — overrides the default scripting language for expr/body</li>
 *   <li>{@code description}, {@code errorCode}, {@code severity}, {@code errorMessage} — optional overrides</li>
 * </ul>
 *
 * @author Max Arulananthan
 * @since 1.0
 */
class PredefinedValidationRuleBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    private final RuliiNamespaceHandler handler;

    PredefinedValidationRuleBeanDefinitionParser(RuliiNamespaceHandler handler) {
        this.handler = handler;
    }

    @Override
    protected Class<?> getBeanClass(Element element) {
        return PredefinedValidationRuleFactoryBean.class;
    }

    @Override
    protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) {
        String name = element.getAttribute("name");
        if (StringUtils.hasText(name)) return name;
        return parserContext.getReaderContext().generateBeanName(definition);
    }

    @Override
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        populate(element, builder, handler.getDefaultLanguage());
    }

    /**
     * Populates a {@link BeanDefinitionBuilder} for {@link PredefinedValidationRuleFactoryBean}
     * from the given predefined rule element. Called both by this parser and by
     * {@link RuleSetBeanDefinitionParser} when processing inline predefined rules inside
     * a {@code <rulii:rules>} block.
     *
     * @param element         the predefined rule element (e.g. {@code <rulii:notNull>})
     * @param builder         the builder to populate
     * @param defaultLanguage the default scripting language from the namespace handler
     */
    static void populate(Element element, BeanDefinitionBuilder builder, String defaultLanguage) {
        String type = element.getLocalName();

        builder.addPropertyValue("type", type);
        builder.addPropertyValue("name", element.getAttribute("name"));
        builder.addPropertyValue("defaultLanguage", defaultLanguage);
        builder.addPropertyValue("valueSource", ValueSource.parse(element));

        String description = element.getAttribute("description");
        String errorCode = element.getAttribute("errorCode");
        String severity = element.getAttribute("severity");
        String errorMessage = element.getAttribute("errorMessage");

        if (StringUtils.hasText(description)) builder.addPropertyValue("description",  description);
        if (StringUtils.hasText(errorCode)) builder.addPropertyValue("errorCode",    errorCode);
        if (StringUtils.hasText(severity)) builder.addPropertyValue("severity",     severity);
        if (StringUtils.hasText(errorMessage)) builder.addPropertyValue("errorMessage", errorMessage);

        parseTypeSpecificAttributes(type, element, builder);
    }

    private static void parseTypeSpecificAttributes(String type, Element element, BeanDefinitionBuilder builder) {
        switch (type) {
            case "min", "decimalMin" ->
                builder.addPropertyValue("min", element.getAttribute("min"));
            case "max", "decimalMax" ->
                builder.addPropertyValue("max", element.getAttribute("max"));
            case "size" -> {
                builder.addPropertyValue("min", element.getAttribute("min"));
                builder.addPropertyValue("max", element.getAttribute("max"));
            }
            case "digits" -> {
                builder.addPropertyValue("maxIntegerDigits", Integer.parseInt(element.getAttribute("maxIntegerDigits")));
                builder.addPropertyValue("maxFractionDigits", Integer.parseInt(element.getAttribute("maxFractionDigits")));
            }
            case "pattern" -> {
                builder.addPropertyValue("pattern", element.getAttribute("pattern"));
                String cs = element.getAttribute("caseSensitive");
                builder.addPropertyValue("caseSensitive", !StringUtils.hasText(cs) || Boolean.parseBoolean(cs));
            }
            case "assertEquals", "assertNotEquals" ->
                builder.addPropertyValue("value", element.getAttribute("value"));
            case "startsWith" ->
                builder.addPropertyValue("items", parseChildTextContent(element, "prefix"));
            case "endsWith" ->
                builder.addPropertyValue("items", parseChildTextContent(element, "suffix"));
            case "in" ->
                builder.addPropertyValue("items", parseChildTextContent(element, "item"));
        }
    }

    private static List<String> parseChildTextContent(Element parent, String childTagName) {
        return DomUtils.getChildElementsByTagName(parent, childTagName)
                .stream()
                .map(e -> e.getTextContent().strip())
                .toList();
    }
}
