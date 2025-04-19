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
package org.rulii.spring;

import org.rulii.model.Runnable;
import org.rulii.model.UnrulyException;
import org.rulii.rule.Rule;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.util.Assert;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RuleBeans class represents a utility for handling Rule instances within a ListableBeanFactory context.
 *
 * A RuleBeans instance must be initialized with a non-null ListableBeanFactory.
 * It allows retrieving specific Rule instances based on ID or Class type.
 * Additionally, it provides a method for handling context refresh events.
 */
public class RuleBeans {

    private ListableBeanFactory ctx;

    public RuleBeans(ListableBeanFactory ctx) {
        super();
        Assert.notNull(ctx, "ctx cannot be null.");
        this.ctx = ctx;
    }

    /**
     * Retrieves a Rule instance based on the provided ID.
     *
     * @param id the ID of the Rule to retrieve
     * @return the Rule instance associated with the given ID
     */
    public Rule getRule(String id) {
        return getCtx().getBean(id, Rule.class);
    }

    /**
     * Retrieves a Rule instance based on the provided ruleClass.
     *
     * @param ruleClass the class type of the Rule to be retrieved
     * @return the Rule instance associated with the given ruleClass
     * @throws UnrulyException if no matching Rule is found
     * @throws NoUniqueBeanDefinitionException if multiple matching Rules are found
     */
    public Rule getRule(Class<?> ruleClass) {
        List<Rule> matches = getCtx().getBeansOfType(Rule.class).values()
                .stream()
                .filter(((Rule r) -> r.getTarget() != null && r.getTarget().getClass().equals(ruleClass)))
                .toList();

        if (matches.isEmpty()) throw new UnrulyException("");
        if (matches.size() > 1) {
            List<String> names = matches.stream()
                    .map(Runnable::getName).collect(Collectors.toList());
            throw new NoUniqueBeanDefinitionException(ruleClass, names, "Multiple matching Rules [" + names + "]. Expected Single, try specifying the RuleName.");
        }

        return matches.get(0);
    }

    /**
     * Handles the ContextClosedEvent by setting the ListableBeanFactory ctx to null.
     * This method is annotated with @EventListener to indicate that it is an event listener for ContextClosedEvent.
     *
     * @param ctxClosedEvent the ContextClosedEvent to be handled
     */
    @EventListener
    public void handleContextRefreshEvent(ContextClosedEvent ctxClosedEvent) {
        this.ctx = null;
    }

    /**
     * Retrieve the ListableBeanFactory used by the RuleBeans instance.
     *
     * @return the ListableBeanFactory instance for handling Rule instances within a context
     * @throws UnrulyException if the application context is closed
     */
    private ListableBeanFactory getCtx() {
        if (ctx == null) throw new UnrulyException("Application Context is closed.");
        return ctx;
    }

    @Override
    public String toString() {
        return "RuleBeans{" +
                "ctx=" + ctx +
                '}';
    }
}
