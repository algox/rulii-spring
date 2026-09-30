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

import org.rulii.bind.Binding;
import org.rulii.bind.Bindings;
import org.rulii.bind.ScopedBindings;
import org.rulii.ruleflow.RuleFlowBuilderTemplate;
import org.rulii.ruleflow.command.ContainerCommand;
import org.rulii.ruleflow.info.CommandInfo;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.function.Consumer;

/**
 * RuleFlow builder used by {@link RuleFlowFactoryBean} to construct XML-declared flows.
 * Extends the core builder template (the intended extension axis) to expose the
 * {@code runContainer} hook, so {@code <r:command ref="...">} elements with a nested
 * body can drive user-supplied {@link ContainerCommand} beans (retry, parallel, ...).
 *
 * @author Max Arulananthan
 * @since 2.0
 */
class SpringRuleFlowBuilder extends RuleFlowBuilderTemplate<SpringRuleFlowBuilder> {

    SpringRuleFlowBuilder() {
        super();
    }

    @Override
    protected SpringRuleFlowBuilder newInstance() {
        return new SpringRuleFlowBuilder();
    }

    /**
     * Plugs in a custom container command with the given body — the XML-facing bridge
     * to the protected {@link #runContainer} extension hook.
     *
     * @param cmd  the container command bean; must not be null
     * @param body Consumer defining the body commands; must not be null
     * @return this builder
     */
    SpringRuleFlowBuilder container(ContainerCommand cmd, Consumer<SpringRuleFlowBuilder> body) {
        return runContainer(cmd, body);
    }

    /**
     * Binds a Spring bean referenced by {@code <r:bind ref="...">}: a {@link Bindings} bean
     * is copied binding by binding, a {@link Map} bean is loaded by entry, and any other bean
     * by its properties - the same behaviour as the core {@code bind(Bindings)} and
     * {@code bind(Object)} overloads. The step's {@link CommandInfo.Bind} carries the bean
     * name as its label, so the flow's definition names what the XML names.
     *
     * @param scope    the target scope; null for the current scope
     * @param beanName the referenced bean's name; must not be empty
     * @param bean     the resolved bean; must not be null
     * @return this builder
     * @since 2.1
     */
    @SuppressWarnings("unchecked")
    SpringRuleFlowBuilder bindBean(String scope, String beanName, Object bean) {
        Assert.hasText(beanName, "beanName cannot be empty/null.");
        Assert.notNull(bean, "bean cannot be null.");

        if (bean instanceof Bindings source) {
            return bindWith(bindings -> {
                Bindings target = target(bindings, scope);
                for (Binding<?> b : source) target.bind(b);
            }, () -> labelled(CommandInfo.Bind.bindings(scope, source), beanName));
        }

        return bindWith(bindings -> {
            Bindings target = target(bindings, scope);
            if (bean instanceof Map<?, ?> m) target.loadMap((Map<String, Object>) m);
            else target.loadProperties(bean);
        }, () -> labelled(CommandInfo.Bind.object(scope, bean), beanName));
    }

    private static Bindings target(ScopedBindings bindings, String scope) {
        return scope != null ? bindings.getScopeBindings(scope) : bindings;
    }

    private static CommandInfo.Bind labelled(CommandInfo.Bind bind, String label) {
        return new CommandInfo.Bind(bind.scope(), bind.kind(), bind.names(), label);
    }
}
