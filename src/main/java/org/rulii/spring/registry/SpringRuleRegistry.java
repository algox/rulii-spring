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
package org.rulii.spring.registry;

import org.rulii.model.Runnable;
import org.rulii.model.UnrulyException;
import org.rulii.registry.RuleRegistry;
import org.rulii.rule.Rule;
import org.rulii.ruleflow.RuleFlow;
import org.rulii.ruleset.RuleSet;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a registry for managing rules within a Spring ApplicationContext.
 * Provides methods for handling rules, rule sets, and rule flows.
 *
 * <p>Lookups are hierarchy-consistent: enumeration ({@link #getRules()}, {@link #getRuleSets()},
 * {@link #getRuleFlows()}, {@link #getCount()}) includes ancestor bean factories, matching the
 * by-name lookups. Bean-name resolution per type is cached (bean definitions are frozen after
 * context refresh); the cache is released when the context closes.
 *
 * @author Max Arulananthan
 * @since 1.0
 *
 */
public class SpringRuleRegistry implements RuleRegistry {

    private volatile ListableBeanFactory ctx;
    private final Map<Class<?>, String[]> beanNameCache = new ConcurrentHashMap<>();

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
        return beanNames(Runnable.class).length;
    }

    @Override
    public List<Rule> getRules() {
        return beansOf(Rule.class);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List<RuleSet> getRuleSets() {
        return beansOf(RuleSet.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public List<RuleFlow<?>> getRuleFlows() {
        return (List) beansOf(RuleFlow.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R, T extends Runnable<R>> T get(String name) {
        return (T) get(name, Runnable.class);
    }

    @Override
    public <R, T extends Runnable<R>> T get(String name, Class<T> type) {
        Assert.notNull(name, "name cannot be null.");
        Assert.notNull(type, "type cannot be null.");
        ListableBeanFactory ctx = getCtx();
        // isTypeMatch answers from metadata - no exception-driven control flow and no
        // instantiation of beans that turn out to be of a different type.
        if (!ctx.containsBean(name) || !ctx.isTypeMatch(name, type)) return null;
        return ctx.getBean(name, type);
    }

    /**
     * Returns the bean names for the given type, including ancestor bean factories.
     * Results are cached - bean definitions are frozen once the context is refreshed.
     *
     * @param type the type to look up
     * @return the matching bean names
     */
    private String[] beanNames(Class<?> type) {
        return beanNameCache.computeIfAbsent(type,
                t -> BeanFactoryUtils.beanNamesForTypeIncludingAncestors(getCtx(), t, true, true));
    }

    /**
     * Resolves all beans of the given type via the cached name list.
     *
     * @param type the type to resolve
     * @param <T>  the bean type
     * @return the resolved beans
     */
    private <T> List<T> beansOf(Class<T> type) {
        ListableBeanFactory ctx = getCtx();
        return Arrays.stream(beanNames(type)).map(name -> ctx.getBean(name, type)).toList();
    }

    /**
     * Retrieves the application context.
     *
     * @return The application context instance.
     */
    private ListableBeanFactory getCtx() {
        ListableBeanFactory result = ctx;
        if (result == null) throw new UnrulyException("Application Context is closed.");
        return result;
    }

    /**
     * Handles the ContextClosedEvent by releasing the ApplicationContext and the name cache.
     *
     * @param ctxClosedEvent the ContextClosedEvent to be handled
     */
    @EventListener
    public void onContextClosed(ContextClosedEvent ctxClosedEvent) {
        this.ctx = null;
        this.beanNameCache.clear();
    }

    @Override
    public String toString() {
        return "SpringRuleRegistry{" +
                "ctx=" + ctx +
                '}';
    }
}
