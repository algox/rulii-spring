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

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Declarative mirror of a rulii {@code RuleFlowCommand}, parsed from a {@code <r:ruleflow>}
 * XML element. {@link RuleFlowBeanDefinitionParser} builds a tree of these (container
 * commands nest command sequences); {@link RuleFlowFactoryBean} walks the tree once at
 * bean initialisation, driving {@code RuleFlow.builder()}.
 *
 * <p>Bean references are stored as bean <em>names</em> and resolved by the factory bean
 * (bean-refs live at arbitrary depth inside this tree, where Spring's automatic
 * {@code RuntimeBeanReference} resolution does not reach).
 *
 * @author Max Arulananthan
 * @since 2.0
 */
public sealed interface RuleFlowCommandDefinition
        permits RuleFlowCommandDefinition.Bind, RuleFlowCommandDefinition.Run,
                RuleFlowCommandDefinition.Apply, RuleFlowCommandDefinition.Execute,
                RuleFlowCommandDefinition.AsyncRun, RuleFlowCommandDefinition.Await,
                RuleFlowCommandDefinition.When, RuleFlowCommandDefinition.ForEach,
                RuleFlowCommandDefinition.Scope, RuleFlowCommandDefinition.Exit,
                RuleFlowCommandDefinition.Custom {

    /** A step-scoped literal parameter: {@code <r:with name="x" value="10"/>}. */
    record WithParam(String name, String value) { }

    /**
     * An exception handler: {@code <r:on-exception exception="fqcn">body</r:on-exception>}.
     * Used step-level (inside run/apply/execute/async-run) and flow-level (global).
     */
    record Handler(String exceptionClass, List<RuleFlowCommandDefinition> body) { }

    /** Async continuation: {@code <r:then-run result="x">body</r:then-run>}. */
    record ThenRun(String result, List<RuleFlowCommandDefinition> body) { }

    /**
     * {@code <r:bind name="x" .../>} — exactly one source: literal {@code value},
     * {@code ref} (a Bindings or POJO bean), or a script expression (evaluated per
     * execution and bound under {@code name}).
     */
    record Bind(String name, String scope, String value, String beanRef,
                ScriptExpression expression) implements RuleFlowCommandDefinition { }

    /**
     * {@code <r:run .../>} — exactly one target: {@code bean-ref} (Spring bean, resolved
     * at wiring time), {@code name} (RuleRegistry lookup at execution time), or
     * {@code class} (RuleRegistry lookup by rule class at execution time).
     */
    record Run(String beanRef, String nameInRegistry, String className,
               String as, String scope, List<WithParam> params,
               Handler handler) implements RuleFlowCommandDefinition { }

    /** {@code <r:apply>expr</r:apply>} — a Function command. */
    record Apply(ScriptExpression expression, String as, String scope,
                 List<WithParam> params, Handler handler) implements RuleFlowCommandDefinition { }

    /** {@code <r:execute>expr</r:execute>} — an Action command. */
    record Execute(ScriptExpression expression, Handler handler) implements RuleFlowCommandDefinition { }

    /** {@code <r:async-run .../>} — same targets as {@link Run}, plus async config. */
    record AsyncRun(String beanRef, String nameInRegistry, String className,
                    String as, boolean immutableBindings, ThenRun thenRun,
                    Handler handler) implements RuleFlowCommandDefinition { }

    /** {@code <r:await/await-all/await-any .../>}. */
    record Await(Kind kind, List<String> names, Long timeout, TimeUnit unit) implements RuleFlowCommandDefinition {
        public enum Kind { ONE, ALL, ANY }
    }

    /** {@code <r:when>} — condition with a then-body and optional otherwise-body. */
    record When(ScriptExpression condition, List<RuleFlowCommandDefinition> thenBody,
                List<RuleFlowCommandDefinition> otherwiseBody) implements RuleFlowCommandDefinition { }

    /** {@code <r:for-each item="x">} — source function, optional stop-condition, body. */
    record ForEach(String item, ScriptExpression source, ScriptExpression stopCondition,
                   List<RuleFlowCommandDefinition> body) implements RuleFlowCommandDefinition { }

    /** {@code <r:scope name="?">body</r:scope>}. */
    record Scope(String name, List<RuleFlowCommandDefinition> body) implements RuleFlowCommandDefinition { }

    /** {@code <r:exit/>}. */
    record Exit() implements RuleFlowCommandDefinition { }

    /**
     * {@code <r:command ref="..."/>} — a user-supplied {@code RuleFlowCommand} bean.
     * With a nested body, the bean must be a {@code ContainerCommand} (retry, parallel, …)
     * and receives the body commands; without one, it is plugged in as a leaf command.
     */
    record Custom(String beanRef, List<RuleFlowCommandDefinition> body) implements RuleFlowCommandDefinition { }
}
