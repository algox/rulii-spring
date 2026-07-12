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

import org.rulii.spring.xml.RuleFlowCommandDefinition.Apply;
import org.rulii.spring.xml.RuleFlowCommandDefinition.AsyncRun;
import org.rulii.spring.xml.RuleFlowCommandDefinition.Await;
import org.rulii.spring.xml.RuleFlowCommandDefinition.Bind;
import org.rulii.spring.xml.RuleFlowCommandDefinition.Custom;
import org.rulii.spring.xml.RuleFlowCommandDefinition.Execute;
import org.rulii.spring.xml.RuleFlowCommandDefinition.Exit;
import org.rulii.spring.xml.RuleFlowCommandDefinition.ForEach;
import org.rulii.spring.xml.RuleFlowCommandDefinition.Handler;
import org.rulii.spring.xml.RuleFlowCommandDefinition.Run;
import org.rulii.spring.xml.RuleFlowCommandDefinition.Scope;
import org.rulii.spring.xml.RuleFlowCommandDefinition.ThenRun;
import org.rulii.spring.xml.RuleFlowCommandDefinition.When;
import org.rulii.spring.xml.RuleFlowCommandDefinition.WithParam;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Parses a {@code <rulii:ruleflow>} element and registers a {@link RuleFlowFactoryBean}
 * bean definition whose name equals the element's {@code name} attribute.
 *
 * <p>The flow's command elements (including arbitrarily nested container bodies) are
 * parsed by a single recursive routine into a {@link RuleFlowCommandDefinition} tree;
 * the factory bean drives {@code RuleFlow.builder()} from that tree at bean creation.
 *
 * @author Max Arulananthan
 * @since 2.0
 */
class RuleFlowBeanDefinitionParser extends AbstractRuliiBeanDefinitionParser {

    /** Direct children of <ruleflow> that are flow-level configuration, not commands. */
    private static final Set<String> FLOW_LEVEL_ELEMENTS =
            Set.of("context", "param", "on-exception", "finalizer", "returning");

    RuleFlowBeanDefinitionParser() {
        super();
    }

    @Override
    protected Class<?> getBeanClass(Element element) {
        return RuleFlowFactoryBean.class;
    }

    @Override
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {

        builder.addPropertyValue("name", element.getAttribute("name"));
        builder.addPropertyValue("description", element.getAttribute("description"));
        builder.addPropertyValue("defaultLanguage", RuliiNamespaceHandler.getDefaultLanguage(element));

        // <context ref="..."/> - must be first per the schema sequence
        Element context = DomUtils.getChildElementByTagName(element, "context");
        if (context != null) builder.addPropertyValue("contextRef", requiredAttribute(context, "ref", parserContext));

        // <param> elements (same grammar as <ruleset>)
        List<Element> params = DomUtils.getChildElementsByTagName(element, "param");
        if (!params.isEmpty()) {
            List<RuleSetFactoryBean.InputParameterDefinition> definitions = new ArrayList<>();
            for (Element param : params) {
                definitions.add(RuleSetBeanDefinitionParser.parseParam(param, parserContext));
            }
            builder.addPropertyValue("params", definitions);
        }

        // Commands: every direct child that is not flow-level configuration
        List<RuleFlowCommandDefinition> commands = new ArrayList<>();
        for (Element child : DomUtils.getChildElements(element)) {
            if (FLOW_LEVEL_ELEMENTS.contains(child.getLocalName())) continue;
            commands.add(parseCommand(child, parserContext));
        }
        builder.addPropertyValue("commands", commands);

        // Flow-level trailers
        Element onException = DomUtils.getChildElementByTagName(element, "on-exception");
        if (onException != null) builder.addPropertyValue("globalHandler", parseHandler(onException, parserContext));

        ScriptExpression finalizer = ScriptExpression.fromAttributeOrChild(element, "finalizer", parserContext);
        if (finalizer != null) builder.addPropertyValue("finalizer", finalizer);

        String returningAttribute = element.getAttribute("returning");
        Element returning = DomUtils.getChildElementByTagName(element, "returning");

        if (StringUtils.hasText(returningAttribute) && returning != null) {
            parserContext.getReaderContext().error("<ruleflow> declares [returning] both as an attribute"
                    + " and as a child element; use one or the other.", element);
        }

        if (StringUtils.hasText(returningAttribute)) {
            builder.addPropertyValue("returningDeclared", true);
            builder.addPropertyValue("returningExpression", new ScriptExpression(null, returningAttribute));
        } else if (returning != null) {
            builder.addPropertyValue("returningDeclared", true);
            // <returning/> with no expression maps to builder.returning() - the flow
            // returns its RuleContext; with an expression it maps to returning(Function).
            builder.addPropertyValue("returningExpression", ScriptExpression.tryParse(returning));
        }
    }

    /** Parses a command sequence: every child element of the given container is a command. */
    private List<RuleFlowCommandDefinition> parseCommands(Element container, ParserContext parserContext) {
        List<RuleFlowCommandDefinition> result = new ArrayList<>();
        for (Element child : DomUtils.getChildElements(container)) {
            result.add(parseCommand(child, parserContext));
        }
        return result;
    }

    private RuleFlowCommandDefinition parseCommand(Element element, ParserContext parserContext) {
        return switch (element.getLocalName()) {
            case "bind" -> parseBind(element, parserContext);
            case "run" -> parseRun(element, parserContext);
            case "apply" -> parseApply(element, parserContext);
            case "execute" -> parseExecute(element, parserContext);
            case "async-run" -> parseAsyncRun(element, parserContext);
            case "await" -> parseAwait(element, Await.Kind.ONE, parserContext);
            case "await-all" -> parseAwait(element, Await.Kind.ALL, parserContext);
            case "await-any" -> parseAwait(element, Await.Kind.ANY, parserContext);
            case "when" -> parseWhen(element, parserContext);
            case "for-each" -> parseForEach(element, parserContext);
            case "scope" -> new Scope(optionalAttribute(element, "name"),
                    parseCommands(element, parserContext));
            case "exit" -> new Exit();
            case "command" -> new Custom(requiredAttribute(element, "ref", parserContext),
                    parseCommands(element, parserContext));
            default -> {
                parserContext.getReaderContext().error("Unknown ruleflow command element <"
                        + element.getLocalName() + ">.", element);
                yield new Exit(); // unreachable with the fail-fast reporter
            }
        };
    }

    private Bind parseBind(Element element, ParserContext parserContext) {
        String name = requiredAttribute(element, "name", parserContext);
        String value = optionalAttribute(element, "value");
        String ref = optionalAttribute(element, "ref");
        ScriptExpression expression = ScriptExpression.tryParse(element);

        int sources = (value != null ? 1 : 0) + (ref != null ? 1 : 0) + (expression != null ? 1 : 0);
        if (sources != 1) {
            parserContext.getReaderContext().error("<bind name=\"" + name + "\"> must declare exactly one of:"
                    + " a value attribute, a ref attribute, or a script expression.", element);
        }

        return new Bind(name, optionalAttribute(element, "scope"), value, ref, expression);
    }

    private Run parseRun(Element element, ParserContext parserContext) {
        validateSingleTarget(element, parserContext);

        return new Run(optionalAttribute(element, "bean-ref"),
                optionalAttribute(element, "name"),
                optionalAttribute(element, "class"),
                optionalAttribute(element, "as"),
                optionalAttribute(element, "scope"),
                parseWithParams(element, parserContext),
                parseOptionalHandler(element, parserContext));
    }

    private Apply parseApply(Element element, ParserContext parserContext) {
        return new Apply(ScriptExpression.parse(element, parserContext),
                optionalAttribute(element, "as"),
                optionalAttribute(element, "scope"),
                parseWithParams(element, parserContext),
                parseOptionalHandler(element, parserContext));
    }

    private Execute parseExecute(Element element, ParserContext parserContext) {
        return new Execute(ScriptExpression.parse(element, parserContext),
                parseOptionalHandler(element, parserContext));
    }

    private AsyncRun parseAsyncRun(Element element, ParserContext parserContext) {
        validateSingleTarget(element, parserContext);

        ThenRun thenRun = null;
        Element thenRunElement = DomUtils.getChildElementByTagName(element, "then-run");
        if (thenRunElement != null) {
            thenRun = new ThenRun(requiredAttribute(thenRunElement, "result", parserContext),
                    parseCommands(thenRunElement, parserContext));
        }

        return new AsyncRun(optionalAttribute(element, "bean-ref"),
                optionalAttribute(element, "name"),
                optionalAttribute(element, "class"),
                optionalAttribute(element, "as"),
                "immutable".equals(element.getAttribute("context-mode")),
                thenRun,
                parseOptionalHandler(element, parserContext));
    }

    private Await parseAwait(Element element, Await.Kind kind, ParserContext parserContext) {
        List<String> names = kind == Await.Kind.ONE
                ? List.of(requiredAttribute(element, "name", parserContext))
                : Arrays.stream(StringUtils.commaDelimitedListToStringArray(
                        requiredAttribute(element, "names", parserContext)))
                        .map(String::strip).filter(StringUtils::hasText).toList();

        if (names.isEmpty()) {
            parserContext.getReaderContext().error("<" + element.getLocalName()
                    + "> must declare at least one binding name.", element);
        }

        Long timeout = null;
        TimeUnit unit = TimeUnit.SECONDS;
        String timeoutAttr = optionalAttribute(element, "timeout");

        if (timeoutAttr != null) {
            timeout = Long.parseLong(timeoutAttr);
            String unitAttr = optionalAttribute(element, "unit");
            if (unitAttr != null) unit = TimeUnit.valueOf(unitAttr.toUpperCase(Locale.ROOT));
        }

        return new Await(kind, names, timeout, unit);
    }

    private When parseWhen(Element element, ParserContext parserContext) {
        ScriptExpression condition = ScriptExpression.fromAttributeOrChild(element, "condition", parserContext);
        Element then = DomUtils.getChildElementByTagName(element, "then");
        Element otherwise = DomUtils.getChildElementByTagName(element, "otherwise");

        if (condition == null || then == null) {
            parserContext.getReaderContext().error(
                    "<when> requires a condition (attribute or <condition> child) and a <then> body.", element);
        }

        return new When(condition,
                parseCommands(then, parserContext),
                otherwise != null ? parseCommands(otherwise, parserContext) : null);
    }

    private ForEach parseForEach(Element element, ParserContext parserContext) {
        ScriptExpression source = ScriptExpression.fromAttributeOrChild(element, "source", parserContext);

        if (source == null) {
            parserContext.getReaderContext().error(
                    "<for-each> requires a source function (attribute or <source> child).", element);
        }

        List<RuleFlowCommandDefinition> body = new ArrayList<>();
        for (Element child : DomUtils.getChildElements(element)) {
            String localName = child.getLocalName();
            if ("source".equals(localName) || "stop-condition".equals(localName)) continue;
            body.add(parseCommand(child, parserContext));
        }

        return new ForEach(requiredAttribute(element, "item", parserContext),
                source,
                ScriptExpression.fromAttributeOrChild(element, "stop-condition", parserContext),
                body);
    }

    private List<WithParam> parseWithParams(Element element, ParserContext parserContext) {
        List<WithParam> result = new ArrayList<>();
        for (Element with : DomUtils.getChildElementsByTagName(element, "with")) {
            result.add(new WithParam(requiredAttribute(with, "name", parserContext),
                    requiredAttribute(with, "value", parserContext)));
        }
        return result;
    }

    private Handler parseOptionalHandler(Element element, ParserContext parserContext) {
        Element handler = DomUtils.getChildElementByTagName(element, "on-exception");
        return handler != null ? parseHandler(handler, parserContext) : null;
    }

    private Handler parseHandler(Element element, ParserContext parserContext) {
        String exceptionClass = optionalAttribute(element, "exception");
        return new Handler(exceptionClass != null ? exceptionClass : "java.lang.Exception",
                parseCommands(element, parserContext));
    }

    private void validateSingleTarget(Element element, ParserContext parserContext) {
        int targets = (StringUtils.hasText(element.getAttribute("bean-ref")) ? 1 : 0)
                + (StringUtils.hasText(element.getAttribute("name")) ? 1 : 0)
                + (StringUtils.hasText(element.getAttribute("class")) ? 1 : 0);

        if (targets != 1) {
            parserContext.getReaderContext().error("<" + element.getLocalName()
                    + "> must declare exactly one target: bean-ref (Spring bean), name (registry lookup),"
                    + " or class (registry lookup by rule class).", element);
        }
    }

    private String requiredAttribute(Element element, String name, ParserContext parserContext) {
        String value = element.getAttribute(name);
        if (!StringUtils.hasText(value)) {
            parserContext.getReaderContext().error("<" + element.getLocalName()
                    + "> requires a non-empty [" + name + "] attribute.", element);
        }
        return value;
    }

    private String optionalAttribute(Element element, String name) {
        String value = element.getAttribute(name);
        return StringUtils.hasText(value) ? value : null;
    }
}
