/*
 * This software is licensed under the Apache 2 license, quoted below.
 *
 * Copyright (c) 1999-2025, Algorithmx Inc.
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
package org.rulii.spring.registry;

import org.rulii.model.Runnable;
import org.rulii.model.UnrulyException;
import org.rulii.registry.RuleRegistry;
import org.rulii.rule.Rule;
import org.rulii.ruleset.RuleSet;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.util.Assert;

import java.util.List;

/**
 * Represents a registry for managing rules within a Spring ApplicationContext.
 * Provides methods for handling rules and rule sets.
 *
 * @author Max Arulananthan
 * @since 1.0
 *
 */
public class SpringRuleRegistry implements RuleRegistry {

    private ListableBeanFactory ctx;

    /**
     * Initializes a new SpringRuleRegistry with the specified ApplicationContext.
     *
     * @param ctx the ApplicationContext to be used
     */
    public SpringRuleRegistry(ListableBeanFactory ctx) {
        super();
        Assert.notNull(ctx, "ctx cannot be null.");
        this.ctx = ctx;
    }
    @Override
    public boolean isNameInUse(String name) {
        Assert.notNull(name, "name cannot be null.");
        return getCtx().containsBean(name);
    }

    @Override
    public int getCount() {
        return getCtx().getBeansOfType(Runnable.class).size();
    }

    @Override
    public List<Rule> getRules() {
        return getCtx().getBeansOfType(Rule.class).values().stream().toList();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List<RuleSet> getRuleSets() {
        return getCtx().getBeansOfType(RuleSet.class).values().stream().toList();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R, T extends Runnable<R>> T get(String name) {
        Assert.notNull(name, "name cannot be null.");
        return (T) get(name, Runnable.class);
    }

    @Override
    public <R, T extends Runnable<R>> T get(String name, Class<T> type) {
        Assert.notNull(name, "name cannot be null.");
        Assert.notNull(type, "type cannot be null.");
        return getCtx().getBean(name, type);
    }

    /**
     * Retrieves the application context.
     *
     * @return The application context instance.
     */
    private ListableBeanFactory getCtx() {
        if (ctx == null) throw new UnrulyException("Application Context is closed.");
        return ctx;
    }

    /**
     * Handles the ContextClosedEvent by setting the ApplicationContext to null.
     *
     * @param ctxClosedEvent the ContextClosedEvent to be handled
     */
    @EventListener
    public void handleContextRefreshEvent(ContextClosedEvent ctxClosedEvent) {
        this.ctx = null;
    }

    @Override
    public String toString() {
        return "SpringRuleRegistry{" +
                "ctx=" + ctx +
                '}';
    }
}
