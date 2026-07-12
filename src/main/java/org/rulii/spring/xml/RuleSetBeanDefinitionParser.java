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

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

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
 *       {@code <validationRule>}, {@code <bean-ref>}, and {@code <class-ref>} entries</li>
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
class RuleSetBeanDefinitionParser extends AbstractRuliiBeanDefinitionParser {

    RuleSetBeanDefinitionParser() {
        super();
    }

    @Override
    protected Class<?> getBeanClass(Element element) {
        return RuleSetFactoryBean.class;
    }

    @Override
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {

        builder.addPropertyValue("name", element.getAttribute("name"));
        builder.addPropertyValue("description", element.getAttribute("description"));
        builder.addPropertyValue("defaultLanguage", RuliiNamespaceHandler.getDefaultLanguage(element));
        builder.addPropertyValue("validating",
                RuliiNamespaceHandler.parseBooleanAttribute(element.getAttribute("validating"), false));

        // <param> elements
        List<Element> params = DomUtils.getChildElementsByTagName(element, "param");

        if (!params.isEmpty()) {
            List<RuleSetFactoryBean.InputParameterDefinition> inputParameterDefinitions = new ArrayList<>();

            for (Element param : params) {
                inputParameterDefinitions.add(parseParam(param, parserContext));
            }

            builder.addPropertyValue("params", inputParameterDefinitions);
        }

        // <pre-condition> child or pre-condition="..." attribute
        ScriptExpression preCondition = ScriptExpression.fromAttributeOrChild(element, "pre-condition", parserContext);
        if (preCondition != null) builder.addPropertyValue("preCondition", preCondition);

        // <initializer> child or initializer="..." attribute
        ScriptExpression initializer = ScriptExpression.fromAttributeOrChild(element, "initializer", parserContext);
        if (initializer != null) builder.addPropertyValue("initializer", initializer);

        // <rules>
        Element rules = DomUtils.getChildElementByTagName(element, "rules");
        if (rules != null) {
            ManagedList<RuntimeBeanReference> ruleRefs = parseRules(rules, parserContext);
            builder.addPropertyValue("rules", ruleRefs);
        }

        // <stop-condition> child or stop-condition="..." attribute
        ScriptExpression stopCondition = ScriptExpression.fromAttributeOrChild(element, "stop-condition", parserContext);
        if (stopCondition != null) {
            builder.addPropertyValue("stopCondition", stopCondition);
        }

        // <finalizer> child or finalizer="..." attribute
        ScriptExpression finalizer = ScriptExpression.fromAttributeOrChild(element, "finalizer", parserContext);
        if (finalizer != null) {
            builder.addPropertyValue("finalizer", finalizer);
        }

        // <result-extractor>
        Element resultExtractor = DomUtils.getChildElementByTagName(element, "result-extractor");
        if (resultExtractor != null) {
            builder.addPropertyValue("resultExtractor", ScriptExpression.parse(resultExtractor, parserContext));
        }

        // <error-handler>
        Element errorHandler = DomUtils.getChildElementByTagName(element, "error-handler");
        if (errorHandler != null) {
            builder.addPropertyValue("errorHandler", ScriptExpression.parse(errorHandler, parserContext));
        }
    }

    /** Parses all child entries of a {@code <rules>} element into bean references. */
    private ManagedList<RuntimeBeanReference> parseRules(Element rulesEl, ParserContext parserContext) {

        ManagedList<RuntimeBeanReference> refs = new ManagedList<>();

        for (Element child : DomUtils.getChildElements(rulesEl)) {
            String localName = child.getLocalName();

            if ("rule".equals(localName)) {
                refs.add(registerInlineRule(child, parserContext));
            } else if ("validationRule".equals(localName)) {
                refs.add(registerInlineValidationRule(child, parserContext));
            } else if ("bean-ref".equals(localName)) {
                refs.add(new RuntimeBeanReference(child.getAttribute("name")));
            } else if ("class-ref".equals(localName)) {
                refs.add(registerClassRef(child, parserContext));
            } else {
                // Any other element is treated as an inline predefined validation rule
                // (notNull, min, pattern, in, etc.) — delegate to the shared populate helper.
                refs.add(registerInlinePredefinedValidationRule(child, parserContext));
            }
        }
        return refs;
    }

    /**
     * Parses an inline predefined validation rule element (e.g. {@code <rulii:notNull>},
     * {@code <rulii:min>}), registers a {@link PredefinedValidationRuleFactoryBean} sibling
     * bean definition, and returns a {@link RuntimeBeanReference} to it.
     */
    private RuntimeBeanReference registerInlinePredefinedValidationRule(Element el, ParserContext parserContext) {
        BeanDefinitionBuilder rb = BeanDefinitionBuilder.genericBeanDefinition(PredefinedValidationRuleFactoryBean.class);
        PredefinedValidationRuleBeanDefinitionParser.populate(el, rb, parserContext);
        return registerAndRef(rb.getBeanDefinition(), el.getAttribute("name"), el, parserContext);
    }

    /**
     * Parses an inline {@code <rule>} element via the shared
     * {@link RuleBeanDefinitionParser#populate populate} helper, registers it as a sibling
     * bean definition, and returns a {@link RuntimeBeanReference} to it.
     */
    private RuntimeBeanReference registerInlineRule(Element rule, ParserContext parserContext) {
        BeanDefinitionBuilder rb = BeanDefinitionBuilder.genericBeanDefinition(RuleFactoryBean.class);
        RuleBeanDefinitionParser.populate(rule, rb, parserContext);
        return registerAndRef(rb.getBeanDefinition(), rule.getAttribute("name"), rule, parserContext);
    }

    /**
     * Parses an inline {@code <validationRule>} element via the shared
     * {@link ValidationRuleBeanDefinitionParser#populate populate} helper, registers it,
     * and returns a reference.
     */
    private RuntimeBeanReference registerInlineValidationRule(Element el, ParserContext parserContext) {
        BeanDefinitionBuilder rb = BeanDefinitionBuilder.genericBeanDefinition(ValidationRuleFactoryBean.class);
        ValidationRuleBeanDefinitionParser.populate(el, rb, parserContext);
        return registerAndRef(rb.getBeanDefinition(), el.getAttribute("name"), el, parserContext);
    }

    /**
     * Parses a {@code <class-ref>} element, registers the rule class as a Spring bean
     * (with any declared constructor args and property injections), wraps it in a
     * {@link RuleFromInstanceFactoryBean}, and returns a reference to the wrapper bean.
     *
     * <p>Spring handles all type conversion (via {@link TypedStringValue}) and bean wiring
     * (via constructor-arg / property references), so no custom reflection code is needed here.
     * The class name is stored on the bean definition and resolved lazily by the container's
     * bean class loader, like any other bean class.
     */
    private RuntimeBeanReference registerClassRef(Element el, ParserContext parserContext) {

        // 1. Build a bean definition for the rule class itself.
        BeanDefinitionBuilder instanceBuilder = BeanDefinitionBuilder.genericBeanDefinition(el.getAttribute("class"));

        for (Element arg : DomUtils.getChildElementsByTagName(el, "arg")) {
            String ref   = arg.getAttribute("ref");
            String value = arg.getAttribute("value");
            String type  = arg.getAttribute("type");
            if (StringUtils.hasText(ref)) {
                instanceBuilder.addConstructorArgReference(ref);
            } else {
                instanceBuilder.addConstructorArgValue(
                        StringUtils.hasText(type) ? new TypedStringValue(value, type) : value);
            }
        }

        for (Element prop : DomUtils.getChildElementsByTagName(el, "property")) {
            String name  = prop.getAttribute("name");
            String ref   = prop.getAttribute("ref");
            String value = prop.getAttribute("value");
            String type  = prop.getAttribute("type");
            if (StringUtils.hasText(ref)) {
                instanceBuilder.addPropertyReference(name, ref);
            } else {
                instanceBuilder.addPropertyValue(name,
                        StringUtils.hasText(type) ? new TypedStringValue(value, type) : value);
            }
        }

        String instanceBeanName = parserContext.getReaderContext()
                .generateBeanName(instanceBuilder.getBeanDefinition());
        parserContext.getRegistry().registerBeanDefinition(instanceBeanName, instanceBuilder.getBeanDefinition());

        // 2. Wrap the instance in a RuleFromInstanceFactoryBean.
        BeanDefinitionBuilder wrapperBuilder =
                BeanDefinitionBuilder.genericBeanDefinition(RuleFromInstanceFactoryBean.class);
        wrapperBuilder.addPropertyReference("ruleInstance", instanceBeanName);
        return registerAndRef(wrapperBuilder.getBeanDefinition(), null, el, parserContext);
    }

    /**
     * Registers a bean definition and returns a {@link RuntimeBeanReference} to it.
     * Uses the provided {@code name} when non-blank, otherwise generates a unique name.
     *
     * <p>An explicit name that is already registered is reported as a parse error (with the
     * source XML location) rather than silently reusing the existing bean — sharing a rule
     * across rulesets is done with {@code <bean-ref>}, never by name collision.
     */
    private RuntimeBeanReference registerAndRef(AbstractBeanDefinition def, String name, Element source, ParserContext parserContext) {
        String beanName = resolveBeanName(name, def, parserContext);

        if (parserContext.getRegistry().containsBeanDefinition(beanName)) {
            parserContext.getReaderContext().error("Duplicate rule bean name [" + beanName
                    + "]: a bean definition with this name already exists."
                    + " Use a unique name, or reference the existing rule with <bean-ref name=\"" + beanName + "\"/>.", source);
        } else {
            parserContext.getRegistry().registerBeanDefinition(beanName, def);
        }

        return new RuntimeBeanReference(beanName);
    }

    /**
     * Parses a {@code <param>} element into a {@link RuleSetFactoryBean.InputParameterDefinition}.
     * Shared with {@link RuleFlowBeanDefinitionParser}, which uses the identical param grammar.
     */
    static RuleSetFactoryBean.InputParameterDefinition parseParam(Element el, ParserContext parserContext) {
        String paramName = el.getAttribute("name");
        String type = el.getAttribute("type");
        boolean required = RuliiNamespaceHandler.parseBooleanAttribute(el.getAttribute("required"), true);

        ScriptExpression defaultValueExpr = null;
        Element defaultValue = DomUtils.getChildElementByTagName(el, "default-value");
        if (defaultValue != null) defaultValueExpr = ScriptExpression.parse(defaultValue, parserContext);

        return new RuleSetFactoryBean.InputParameterDefinition(paramName, type, required, defaultValueExpr);
    }
}
