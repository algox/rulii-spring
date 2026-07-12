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

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * Parses a {@code <rulii:validationRule>} element and registers a
 * {@link ValidationRuleFactoryBean} bean definition.
 *
 * <p>Supported attributes:
 * <ul>
 *   <li>{@code name} (required) — rule and bean name</li>
 *   <li>{@code description} — optional human-readable description</li>
 *   <li>{@code errorCode} (required) — code placed in the {@link org.rulii.validation.RuleViolation}</li>
 *   <li>{@code severity} — one of {@code FATAL}, {@code ERROR} (default), {@code WARNING}, {@code INFO}</li>
 *   <li>{@code errorMessage} — message template (may reference bindings)</li>
 *   <li>{@code defaultMessage} — fallback message when the template cannot be resolved</li>
 * </ul>
 *
 * <p>The {@code <given>} child element provides the boolean condition script.
 *
 * @author Max Arulananthan
 * @since 1.0
 */
class ValidationRuleBeanDefinitionParser extends AbstractRuliiBeanDefinitionParser {

    ValidationRuleBeanDefinitionParser() {
        super();
    }

    @Override
    protected Class<?> getBeanClass(Element element) {
        return ValidationRuleFactoryBean.class;
    }

    @Override
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        populate(element, builder, parserContext);
    }

    /**
     * Populates a {@link BeanDefinitionBuilder} for {@link ValidationRuleFactoryBean} from
     * the given {@code <validationRule>} element. Called both by this parser and by
     * {@link RuleSetBeanDefinitionParser} when processing inline validation rules inside a
     * {@code <rulii:rules>} block, so both declaration styles always parse identically.
     *
     * @param element       the {@code <validationRule>} element
     * @param builder       the builder to populate
     * @param parserContext the parser context used for error reporting
     */
    static void populate(Element element, BeanDefinitionBuilder builder, ParserContext parserContext) {

        builder.addPropertyValue("name", element.getAttribute("name"));
        builder.addPropertyValue("description", element.getAttribute("description"));
        builder.addPropertyValue("defaultLanguage", RuliiNamespaceHandler.getDefaultLanguage(element));

        String errorCode = element.getAttribute("errorCode");
        String severity = element.getAttribute("severity");
        String errorMessage = element.getAttribute("errorMessage");
        String defaultMessage = element.getAttribute("defaultMessage");

        builder.addPropertyValue("errorCode",      errorCode);
        if (StringUtils.hasText(severity)) builder.addPropertyValue("severity",       severity);
        if (StringUtils.hasText(errorMessage)) builder.addPropertyValue("errorMessage",   errorMessage);
        if (StringUtils.hasText(defaultMessage)) builder.addPropertyValue("defaultMessage", defaultMessage);

        ScriptExpression given = ScriptExpression.fromAttributeOrChild(element, "given", parserContext);

        if (given != null) builder.addPropertyValue("condition", given);
    }
}
