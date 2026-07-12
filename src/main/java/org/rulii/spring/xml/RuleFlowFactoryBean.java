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

import org.rulii.bind.Bindings;
import org.rulii.bind.BindingDeclaration;
import org.rulii.model.UnrulyException;
import org.rulii.model.function.Function;
import org.rulii.rule.Rule;
import org.rulii.ruleflow.AsyncRunSpec;
import org.rulii.ruleflow.ExecuteSpec;
import org.rulii.ruleflow.RuleFlow;
import org.rulii.ruleflow.RunSpec;
import org.rulii.ruleflow.command.ContainerCommand;
import org.rulii.ruleflow.command.RuleFlowCommand;
import org.rulii.ruleset.RuleSet;
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
import org.rulii.spring.xml.RuleFlowCommandDefinition.When;
import org.rulii.spring.xml.RuleFlowCommandDefinition.WithParam;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Spring {@link FactoryBean} that constructs a {@link RuleFlow} from configuration declared
 * in a {@code <rulii:ruleflow>} XML element.
 *
 * <p>The parser supplies a tree of {@link RuleFlowCommandDefinition}s; this factory walks
 * the tree once at bean initialisation, driving {@code RuleFlow.builder()} — nested XML
 * bodies map directly onto the builder's {@code Consumer} bodies. Script expressions are
 * compiled through the standard rulii {@code Script.builder()} pipeline via
 * {@link ScriptExpression}.
 *
 * <p>Bean references ({@code bean-ref}, {@code <r:command ref>}, {@code <r:context ref>},
 * {@code <r:bind ref>}) are resolved against the {@link BeanFactory} while the flow bean is
 * created (wiring time); {@code name}/{@code class} run targets are passed through to the
 * builder for {@code RuleRegistry} lookup at execution time.
 *
 * @author Max Arulananthan
 * @since 2.0
 */
public class RuleFlowFactoryBean implements FactoryBean<RuleFlow<?>>, InitializingBean, BeanFactoryAware {

    private String name;
    private String description;
    private String defaultLanguage;

    private String contextRef;
    private List<RuleSetFactoryBean.InputParameterDefinition> params = new ArrayList<>();
    private List<RuleFlowCommandDefinition> commands = new ArrayList<>();
    private Handler globalHandler;
    private ScriptExpression finalizer;
    private boolean returningDeclared;
    private ScriptExpression returningExpression;

    private BeanFactory beanFactory;
    private RuleFlow<?> ruleFlow;

    // Guards against reusing one ContainerCommand instance for several <command> bodies:
    // setBody() mutates the bean, so a shared instance would silently keep only the last
    // body. Container beans should be @Scope("prototype") when referenced more than once.
    private final Set<ContainerCommand> usedContainers = Collections.newSetFromMap(new IdentityHashMap<>());

    public RuleFlowFactoryBean() {
        super();
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void afterPropertiesSet() throws Exception {
        try {
            SpringRuleFlowBuilder builder = new SpringRuleFlowBuilder();
            builder.name(name);
            usedContainers.clear();
            if (StringUtils.hasText(description)) builder.description(description);

            if (contextRef != null) {
                builder.context(beanFactory.getBean(contextRef, Consumer.class));
            }

            for (RuleSetFactoryBean.InputParameterDefinition param : params) {
                Class type = ClassUtils.forName(param.getType(), null);

                if (param.getDefaultValueExpression() != null) {
                    builder.param(param.getName(), type,
                            (Function) param.getDefaultValueExpression().toFunction(defaultLanguage));
                } else {
                    builder.param(param.getName(), type, param.isRequired());
                }
            }

            buildInto(builder, commands);

            if (globalHandler != null) {
                builder.onException(exceptionClass(globalHandler),
                        b -> buildInto(b, globalHandler.body()));
            }

            if (finalizer != null) builder.finalizer(finalizer.toAction(defaultLanguage));

            if (returningDeclared) {
                if (returningExpression != null) {
                    builder.returning(returningExpression.toFunction(defaultLanguage));
                } else {
                    builder.returning();
                }
            }

            ruleFlow = builder.build();
        } catch (UnrulyException e) {
            throw new UnrulyException("Failed to build RuleFlow [" + name + "]: " + e.getMessage(), e);
        }
    }

    /** Recursively drives the builder for a command sequence (top level and nested bodies). */
    private void buildInto(SpringRuleFlowBuilder builder, List<RuleFlowCommandDefinition> defs) {
        for (RuleFlowCommandDefinition def : defs) {
            if (def instanceof Bind bind) {
                buildBind(builder, bind);
            } else if (def instanceof Run run) {
                buildRun(builder, run);
            } else if (def instanceof Apply apply) {
                buildApply(builder, apply);
            } else if (def instanceof Execute execute) {
                buildExecute(builder, execute);
            } else if (def instanceof AsyncRun asyncRun) {
                buildAsyncRun(builder, asyncRun);
            } else if (def instanceof Await await) {
                buildAwait(builder, await);
            } else if (def instanceof When when) {
                buildWhen(builder, when);
            } else if (def instanceof ForEach forEach) {
                buildForEach(builder, forEach);
            } else if (def instanceof Scope scope) {
                buildScope(builder, scope);
            } else if (def instanceof Exit) {
                builder.exit();
            } else if (def instanceof Custom custom) {
                buildCustom(builder, custom);
            }
        }
    }

    private void buildCustom(SpringRuleFlowBuilder builder, Custom custom) {
        RuleFlowCommand command = beanFactory.getBean(custom.beanRef(), RuleFlowCommand.class);

        if (custom.body().isEmpty()) {
            builder.command(command);
            return;
        }

        if (!(command instanceof ContainerCommand container)) {
            throw new UnrulyException("Bean [" + custom.beanRef() + "] referenced by <command> declares a"
                    + " nested body but is not a ContainerCommand (" + command.getClass().getName() + ").");
        }

        if (!usedContainers.add(container)) {
            throw new UnrulyException("ContainerCommand bean [" + custom.beanRef() + "] is used by more than"
                    + " one <command> body - its body would be silently overwritten. Declare the bean with"
                    + " @Scope(\"prototype\") so each usage gets its own instance.");
        }

        builder.container(container, b -> buildInto(b, custom.body()));
    }

    private void buildBind(SpringRuleFlowBuilder builder, Bind bind) {
        if (bind.value() != null) {
            if (bind.scope() != null) {
                builder.bindTo(bind.scope(), bind.name(), bind.value());
            } else {
                builder.bind(bind.name(), bind.value());
            }
            return;
        }

        if (bind.beanRef() != null) {
            Object bean = beanFactory.getBean(bind.beanRef());

            if (bean instanceof Bindings bindings) {
                if (bind.scope() != null) builder.bindTo(bind.scope(), bindings);
                else builder.bind(bindings);
            } else {
                if (bind.scope() != null) builder.bindTo(bind.scope(), bean);
                else builder.bind(bean);
            }
            return;
        }

        // Script expression: evaluated per execution and bound under the given name -
        // semantically an apply(fn).as(name), since bind values are constants in core.
        Function<?> fn = bind.expression().toFunction(defaultLanguage);
        builder.apply(fn, spec -> {
            if (bind.scope() != null) spec.as(bind.scope(), bind.name());
            else spec.as(bind.name());
        });
    }

    private void buildRun(SpringRuleFlowBuilder builder, Run run) {
        Consumer<RunSpec<SpringRuleFlowBuilder>> spec = runSpec(run.as(), run.scope(), run.params(), run.handler());

        if (run.beanRef() != null) {
            Object bean = beanFactory.getBean(run.beanRef());

            if (bean instanceof Rule rule) {
                if (spec != null) builder.run(rule, spec); else builder.run(rule);
            } else if (bean instanceof RuleSet<?> ruleSet) {
                if (spec != null) builder.run(ruleSet, spec); else builder.run(ruleSet);
            } else if (bean instanceof RuleFlow<?> flow) {
                if (spec != null) builder.run(flow, spec); else builder.run(flow);
            } else {
                throw new UnrulyException("Bean [" + run.beanRef() + "] referenced by <run bean-ref> is a ["
                        + bean.getClass().getName() + "]; expected a Rule, RuleSet, or RuleFlow.");
            }
        } else if (run.nameInRegistry() != null) {
            if (spec != null) builder.run(run.nameInRegistry(), spec); else builder.run(run.nameInRegistry());
        } else {
            Class<?> ruleClass = loadClass(run.className(), "<run class>");
            if (spec != null) builder.run(ruleClass, spec); else builder.run(ruleClass);
        }
    }

    private void buildApply(SpringRuleFlowBuilder builder, Apply apply) {
        Consumer<RunSpec<SpringRuleFlowBuilder>> spec = runSpec(apply.as(), apply.scope(), apply.params(), apply.handler());
        Function<?> fn = apply.expression().toFunction(defaultLanguage);

        if (spec != null) builder.apply(fn, spec); else builder.apply(fn);
    }

    private void buildExecute(SpringRuleFlowBuilder builder, Execute execute) {
        if (execute.handler() == null) {
            builder.execute(execute.expression().toAction(defaultLanguage));
            return;
        }

        Consumer<ExecuteSpec<SpringRuleFlowBuilder>> spec = es ->
                es.onException(exceptionClass(execute.handler()), b -> buildInto(b, execute.handler().body()));
        builder.execute(execute.expression().toAction(defaultLanguage), spec);
    }

    private void buildAsyncRun(SpringRuleFlowBuilder builder, AsyncRun asyncRun) {
        Consumer<AsyncRunSpec<SpringRuleFlowBuilder>> spec = asyncSpec(asyncRun);

        if (asyncRun.beanRef() != null) {
            Object bean = beanFactory.getBean(asyncRun.beanRef());

            if (bean instanceof Rule rule) {
                if (spec != null) builder.asyncRun(rule, spec); else builder.asyncRun(rule);
            } else if (bean instanceof RuleSet<?> ruleSet) {
                if (spec != null) builder.asyncRun(ruleSet, spec); else builder.asyncRun(ruleSet);
            } else if (bean instanceof RuleFlow<?> flow) {
                if (spec != null) builder.asyncRun(flow, spec); else builder.asyncRun(flow);
            } else {
                throw new UnrulyException("Bean [" + asyncRun.beanRef() + "] referenced by <async-run bean-ref> is a ["
                        + bean.getClass().getName() + "]; expected a Rule, RuleSet, or RuleFlow.");
            }
        } else if (asyncRun.nameInRegistry() != null) {
            if (spec != null) builder.asyncRun(asyncRun.nameInRegistry(), spec); else builder.asyncRun(asyncRun.nameInRegistry());
        } else {
            Class<?> ruleClass = loadClass(asyncRun.className(), "<async-run class>");
            if (spec != null) builder.asyncRun(ruleClass, spec); else builder.asyncRun(ruleClass);
        }
    }

    private Consumer<AsyncRunSpec<SpringRuleFlowBuilder>> asyncSpec(AsyncRun asyncRun) {
        if (asyncRun.as() == null && !asyncRun.immutableBindings()
                && asyncRun.thenRun() == null && asyncRun.handler() == null) {
            return null;
        }

        return spec -> {
            if (asyncRun.as() != null) spec.as(asyncRun.as());
            if (asyncRun.immutableBindings()) spec.withImmutableBindings();
            if (asyncRun.thenRun() != null) {
                spec.thenRun(asyncRun.thenRun().result(), b -> buildInto(b, asyncRun.thenRun().body()));
            }
            if (asyncRun.handler() != null) {
                spec.onException(exceptionClass(asyncRun.handler()), b -> buildInto(b, asyncRun.handler().body()));
            }
        };
    }

    private void buildAwait(SpringRuleFlowBuilder builder, Await await) {
        String[] names = await.names().toArray(new String[] {});

        switch (await.kind()) {
            case ONE -> {
                if (await.timeout() != null) builder.await(names[0], await.timeout(), await.unit());
                else builder.await(names[0]);
            }
            case ALL -> {
                if (await.timeout() != null) builder.awaitAll(await.timeout(), await.unit(), names);
                else builder.awaitAll(names);
            }
            case ANY -> {
                if (await.timeout() != null) builder.awaitAny(await.timeout(), await.unit(), names);
                else builder.awaitAny(names);
            }
        }
    }

    private void buildWhen(SpringRuleFlowBuilder builder, When when) {
        var condition = when.condition().toCondition(defaultLanguage);

        if (when.otherwiseBody() == null) {
            builder.when(condition, b -> buildInto(b, when.thenBody()));
        } else {
            builder.when(condition, b -> buildInto(b, when.thenBody()), b -> buildInto(b, when.otherwiseBody()));
        }
    }

    private void buildForEach(SpringRuleFlowBuilder builder, ForEach forEach) {
        Function<?> source = forEach.source().toFunction(defaultLanguage);

        if (forEach.stopCondition() == null) {
            builder.forEach(source, forEach.item(), b -> buildInto(b, forEach.body()));
        } else {
            builder.forEach(source, forEach.item(), forEach.stopCondition().toCondition(defaultLanguage),
                    b -> buildInto(b, forEach.body()));
        }
    }

    private void buildScope(SpringRuleFlowBuilder builder, Scope scope) {
        if (scope.name() != null) builder.scope(scope.name(), b -> buildInto(b, scope.body()));
        else builder.scope(b -> buildInto(b, scope.body()));
    }

    /** Builds a RunSpec configurer, or null when the command carries no spec configuration. */
    private Consumer<RunSpec<SpringRuleFlowBuilder>> runSpec(String as, String scope,
                                                              List<WithParam> params, Handler handler) {
        if (as == null && (params == null || params.isEmpty()) && handler == null) return null;

        return spec -> {
            if (as != null) {
                if (scope != null) spec.as(scope, as);
                else spec.as(as);
            }
            if (params != null && !params.isEmpty()) {
                spec.with(params.stream()
                        .map(p -> BindingDeclaration.of(p.name(), (Object) p.value()))
                        .toArray(BindingDeclaration[]::new));
            }
            if (handler != null) {
                spec.onException(exceptionClass(handler), b -> buildInto(b, handler.body()));
            }
        };
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Exception> exceptionClass(Handler handler) {
        Class<?> type = loadClass(handler.exceptionClass(), "<on-exception exception>");

        if (!Exception.class.isAssignableFrom(type)) {
            throw new UnrulyException("Exception handler class [" + handler.exceptionClass()
                    + "] does not extend java.lang.Exception.");
        }

        return (Class<? extends Exception>) type;
    }

    private Class<?> loadClass(String className, String where) {
        try {
            return ClassUtils.forName(className, null);
        } catch (ClassNotFoundException e) {
            throw new UnrulyException("Unable to load class [" + className + "] declared in " + where + ".", e);
        }
    }

    @Override
    public RuleFlow<?> getObject() {
        return ruleFlow;
    }

    @Override
    public Class<?> getObjectType() {
        return RuleFlow.class;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }

    public void setContextRef(String contextRef) {
        this.contextRef = contextRef;
    }

    public void setParams(List<RuleSetFactoryBean.InputParameterDefinition> params) {
        this.params = params != null ? params : new ArrayList<>();
    }

    public void setCommands(List<RuleFlowCommandDefinition> commands) {
        this.commands = commands != null ? commands : new ArrayList<>();
    }

    public void setGlobalHandler(Handler globalHandler) {
        this.globalHandler = globalHandler;
    }

    public void setFinalizer(ScriptExpression finalizer) {
        this.finalizer = finalizer;
    }

    public void setReturningDeclared(boolean returningDeclared) {
        this.returningDeclared = returningDeclared;
    }

    public void setReturningExpression(ScriptExpression returningExpression) {
        this.returningExpression = returningExpression;
    }
}
