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

import org.rulii.spring.config.BeanNames;
import org.rulii.spring.config.RuleBeanBuilder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses a {@code <rulii:ruleset>} element and registers a {@link RuleSetFactoryBean} bean
 * definition whose name equals the element's {@code name} attribute.
 *
 * <p>Supported child elements (all optional):
 * <ul>
 *   <li>{@code <param>} — typed input parameter declarations (repeatable)</li>
 *   <li>{@code <pre-condition>} — guard evaluated before any rule runs</li>
 *   <li>{@code <initializer>} — action executed once before rule processing</li>
 *   <li>{@code <rules>} — ordered container of inline {@code <rule>},
 *       {@code <validationRule>}, and {@code <rule-ref>} entries</li>
 *   <li>{@code <stop-condition>} — condition evaluated between rules to halt execution early</li>
 *   <li>{@code <finalizer>} — action executed once after rule processing completes</li>
 * </ul>
 *
 * <p>Inline {@code <rule>} and {@code <validationRule>} elements inside {@code <rules>} are
 * registered as sibling bean definitions with generated names; the resolved {@link org.rulii.rule.Rule}
 * beans are injected into the {@link RuleSetFactoryBean} via a
 * {@link ManagedList} of {@link RuntimeBeanReference} entries.
 *
 * @author Max Arulananthan
 * @since 1.0
 */
class RuleSetBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    private final RuliiNamespaceHandler handler;

    RuleSetBeanDefinitionParser(RuliiNamespaceHandler handler) {
        this.handler = handler;
    }

    @Override
    protected Class<?> getBeanClass(Element element) {
        return RuleSetFactoryBean.class;
    }

    @Override
    protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) {
        String name = element.getAttribute("name");
        return StringUtils.hasText(name) ? name : parserContext.getReaderContext().generateBeanName(definition);
    }

    @Override
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {

        builder.addPropertyValue("name", element.getAttribute("name"));
        builder.addPropertyValue("description", element.getAttribute("description"));
        builder.addPropertyValue("defaultLanguage", handler.getDefaultLanguage());

        // <param> elements
        List<Element> params = DomUtils.getChildElementsByTagName(element, "param");

        if (!params.isEmpty()) {
            List<RuleSetFactoryBean.InputParameterDefinition> inputParameterDefinitions = new ArrayList<>();

            for (Element param : params) {
                inputParameterDefinitions.add(parseParam(param));
            }

            builder.addPropertyValue("params", inputParameterDefinitions);
        }

        // <pre-condition>
        Element preCond = DomUtils.getChildElementByTagName(element, "pre-condition");
        if (preCond != null) builder.addPropertyValue("preCondition", ScriptExpression.parse(preCond));

        // <initializer>
        Element init = DomUtils.getChildElementByTagName(element, "initializer");
        if (init != null) builder.addPropertyValue("initializer", ScriptExpression.parse(init));

        // <rules>
        Element rules = DomUtils.getChildElementByTagName(element, "rules");
        if (rules != null) {
            ManagedList<RuntimeBeanReference> ruleRefs = parseRules(rules, parserContext);
            builder.addPropertyValue("rules", ruleRefs);
        }

        // <stop-condition>
        Element stop = DomUtils.getChildElementByTagName(element, "stop-condition");
        if (stop != null) {
            builder.addPropertyValue("stopCondition", ScriptExpression.parse(stop));
        }

        // <finalizer>
        Element finalizer = DomUtils.getChildElementByTagName(element, "finalizer");
        if (finalizer != null) {
            builder.addPropertyValue("finalizer", ScriptExpression.parse(finalizer));
        }

        // <result-extractor>
        Element resultExtractor = DomUtils.getChildElementByTagName(element, "result-extractor");
        if (resultExtractor != null) {
            builder.addPropertyValue("resultExtractor", ScriptExpression.parse(resultExtractor));
        }

        // <result-extractor>
        Element errorHandler = DomUtils.getChildElementByTagName(element, "error-handler");
        if (errorHandler != null) {
            builder.addPropertyValue("errorHandler", ScriptExpression.parse(errorHandler));
        }
    }

    /** Parses all child entries of a {@code <rules>} element into bean references. */
    private ManagedList<RuntimeBeanReference> parseRules(Element rulesEl, ParserContext parserContext) {

        ManagedList<RuntimeBeanReference> refs = new ManagedList<>();
        NodeList children = rulesEl.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (!(node instanceof Element child)) continue;

            String localName = child.getLocalName();

            if ("rule".equals(localName)) {
                refs.add(registerInlineRule(child, parserContext));
            } else if ("validationRule".equals(localName)) {
                refs.add(registerInlineValidationRule(child, parserContext));
            } else if ("rule-ref".equals(localName)) {
                RuntimeBeanReference ref = resolveRuleRef(child, parserContext);
                if (ref != null) refs.add(ref);
            }
        }
        return refs;
    }

    /**
     * Parses an inline {@code <rule>} element, registers it as a sibling bean definition,
     * and returns a {@link RuntimeBeanReference} to it.
     */
    private RuntimeBeanReference registerInlineRule(Element rule, ParserContext parserContext) {

        BeanDefinitionBuilder rb = BeanDefinitionBuilder.genericBeanDefinition(RuleFactoryBean.class);

        rb.addPropertyValue("name", rule.getAttribute("name"));
        rb.addPropertyValue("description", rule.getAttribute("description"));
        rb.addPropertyValue("defaultLanguage", handler.getDefaultLanguage());

        Element preCond = DomUtils.getChildElementByTagName(rule, "pre-condition");
        if (preCond != null) rb.addPropertyValue("preCondition", ScriptExpression.parse(preCond));

        Element given = DomUtils.getChildElementByTagName(rule, "given");
        if (given != null) rb.addPropertyValue("condition", ScriptExpression.parse(given));

        List<Element> actions = DomUtils.getChildElementsByTagName(rule, "then");
        if (!actions.isEmpty()) {
            ManagedList<ScriptExpression> thenActions = new ManagedList<>();
            actions.forEach(action -> thenActions.add(ScriptExpression.parse(action)));
            rb.addPropertyValue("thenActions", thenActions);
        }

        Element otherwise = DomUtils.getChildElementByTagName(rule, "otherwise");
        if (otherwise != null) rb.addPropertyValue("otherwiseAction", ScriptExpression.parse(otherwise));

        return registerAndRef(rb.getBeanDefinition(), rule.getAttribute("name"), parserContext);
    }

    /**
     * Parses an inline {@code <validationRule>} element, registers it, and returns a reference.
     */
    private RuntimeBeanReference registerInlineValidationRule(Element el, ParserContext parserContext) {

        BeanDefinitionBuilder rb = BeanDefinitionBuilder.genericBeanDefinition(ValidationRuleFactoryBean.class);
        rb.addPropertyValue("name", el.getAttribute("name"));
        rb.addPropertyValue("description", el.getAttribute("description"));
        rb.addPropertyValue("defaultLanguage", handler.getDefaultLanguage());

        String errorCode = el.getAttribute("errorCode");
        String severity = el.getAttribute("severity");
        String errorMessage = el.getAttribute("errorMessage");
        String defaultMessage = el.getAttribute("defaultMessage");

        rb.addPropertyValue("errorCode", errorCode);
        if (StringUtils.hasText(severity)) rb.addPropertyValue("severity",       severity);
        if (StringUtils.hasText(errorMessage)) rb.addPropertyValue("errorMessage",   errorMessage);
        if (StringUtils.hasText(defaultMessage)) rb.addPropertyValue("defaultMessage", defaultMessage);

        Element condition = DomUtils.getChildElementByTagName(el, "given");
        if (condition != null) rb.addPropertyValue("condition", ScriptExpression.parse(condition));

        return registerAndRef(rb.getBeanDefinition(), el.getAttribute("name"), parserContext);
    }

    /**
     * Resolves a {@code <rule-ref>} element to a {@link RuntimeBeanReference}.
     * Supports {@code beanName} (direct reference) and {@code className} (class-based rule auto-registration).
     */
    private RuntimeBeanReference resolveRuleRef(Element el, ParserContext parserContext) {
        String beanName  = el.getAttribute("beanName");
        String className = el.getAttribute("className");

        if (StringUtils.hasText(beanName)) {
            return new RuntimeBeanReference(beanName);
        }

        if (StringUtils.hasText(className)) {
            try {
                Class<?> ruleClass = ClassUtils.forName(className, Thread.currentThread().getContextClassLoader());
                BeanDefinitionBuilder rb = BeanDefinitionBuilder.genericBeanDefinition(RuleBeanBuilder.class);
                rb.addConstructorArgValue(ruleClass);
                rb.addConstructorArgReference(BeanNames.OBJECT_FACTORY);
                rb.setFactoryMethod("build");
                return registerAndRef(rb.getBeanDefinition(), null, parserContext);
            } catch (ClassNotFoundException e) {
                parserContext.getReaderContext().error("Cannot find rule class '" + className + "'", el);
            }
        } else {
            parserContext.getReaderContext().error("<rule-ref> must specify either 'beanName' or 'className'", el);
        }

        return null;
    }

    /**
     * Registers a bean definition and returns a {@link RuntimeBeanReference} to it.
     * Uses the provided {@code name} when non-blank, otherwise generates a unique name.
     */
    private RuntimeBeanReference registerAndRef(AbstractBeanDefinition def, String name, ParserContext parserContext) {
        String beanName = StringUtils.hasText(name) ? name : parserContext.getReaderContext().generateBeanName(def);

        if (!parserContext.getRegistry().containsBeanDefinition(beanName)) {
            parserContext.getRegistry().registerBeanDefinition(beanName, def);
        }

        return new RuntimeBeanReference(beanName);
    }

    /** Parses a {@code <param>} element into a {@link RuleSetFactoryBean.InputParameterDefinition}. */
    private RuleSetFactoryBean.InputParameterDefinition parseParam(Element el) {
        String paramName = el.getAttribute("name");
        String type = el.getAttribute("type");
        String requiredAttr = el.getAttribute("required");
        boolean required = !StringUtils.hasText(requiredAttr) || Boolean.parseBoolean(requiredAttr);

        ScriptExpression defaultValueExpr = null;
        Element defaultValue = DomUtils.getChildElementByTagName(el, "default-value");
        if (defaultValue != null) defaultValueExpr = ScriptExpression.parse(defaultValue);

        return new RuleSetFactoryBean.InputParameterDefinition(paramName, type, required, defaultValueExpr);
    }
}
