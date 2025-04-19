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
package org.rulii.spring.factory;

import org.rulii.model.UnrulyException;
import org.rulii.util.reflect.DefaultObjectFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;

/**
 * An extension of DefaultObjectFactory that integrates with a Spring ListableBeanFactory to create instances of rules.
 * Used for creating rule instances by leveraging the Spring IoC container.
 *
 * @author Max Arulananthan
 * @since 1.0
 *
 */
public class SpringObjectFactory extends DefaultObjectFactory {

    // Underlying Spring Factory that does the real work.
    private ListableBeanFactory ctx;

    /**
     * Constructs a new SpringObjectFactory with the specified ListableBeanFactory.
     *
     * @param ctx the ListableBeanFactory to integrate with Spring for creating instances of rules
     */
    public SpringObjectFactory(ListableBeanFactory ctx) {
        super(false);
        this.ctx = ctx;
    }

    /**
     * Creates an instance of the specified rule class by leveraging the Spring IoC container.
     *
     * @param ruleClass the class representing the rule to be created
     * @return an instance of the specified rule class created by the Spring IoC container
     * @throws UnrulyException if an issue occurs during the creation of the rule instance
     */
    public <T> T createRule(Class<T> ruleClass) throws UnrulyException {

        if (ctx instanceof AutowireCapableBeanFactory autowireCapableBeanFactory) {
            return autowireCapableBeanFactory.createBean(ruleClass);
        }

        return super.createRule(ruleClass);
    }

    /**
     * Handles the context refresh event triggered by the closure of the context.
     *
     * @param ctxClosedEvent the ContextClosedEvent object that represents the event triggered by the closure of the context
     */
    @EventListener
    public void handleContextRefreshEvent(ContextClosedEvent ctxClosedEvent) {
        this.ctx = null;
    }

    @Override
    public String toString() {
        return "SpringObjectFactory{" +
                "ctx=" + ctx +
                '}';
    }
}
