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
package org.rulii.spring.factory;

import org.rulii.model.UnrulyException;
import org.rulii.util.reflect.DefaultObjectFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An extension of DefaultObjectFactory that integrates with a Spring
 * {@link AutowireCapableBeanFactory} to create instances of rules.
 * Used for creating rule instances by leveraging the Spring IoC container, so
 * {@code @Autowired}, {@code @Value}, and the full BeanPostProcessor chain apply.
 *
 * <p>When {@code isUseCache} is requested (as the base class does for stateless helper
 * types like binding-matching strategies, which are resolved per rule execution), created
 * instances are cached per class; the cache is cleared when the application context closes.
 *
 * @author Max Arulananthan
 * @since 1.0
 *
 */
public class SpringObjectFactory extends DefaultObjectFactory {

    // Underlying Spring Factory that does the real work.
    private AutowireCapableBeanFactory ctx;

    private final Map<Class<?>, Object> objectCache = new ConcurrentHashMap<>();

    /**
     * Constructs a new SpringObjectFactory with the specified AutowireCapableBeanFactory.
     *
     * @param ctx the AutowireCapableBeanFactory used to create and autowire rule instances
     */
    public SpringObjectFactory(AutowireCapableBeanFactory ctx) {
        super(true);
        this.ctx = ctx;
    }

    /**
     * Creates an instance of the specified type by leveraging the underlying Spring IoC container.
     *
     * @param <T> the type of the instance to be created
     * @param type the class representing the type of object to be created
     * @param isUseCache when true, a previously created instance of the same class is reused
     * @return an instance of the specified type created by the Spring IoC container
     * @throws UnrulyException if the context is closed or Spring fails to create the instance
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T create(Class<T> type, boolean isUseCache) {
        if (ctx == null) throw new UnrulyException("Application Context is closed. Cannot create new instances of rules.");

        try {
            if (isUseCache) return (T) objectCache.computeIfAbsent(type, ctx::createBean);
            return ctx.createBean(type);
        } catch (BeansException e) {
            throw new UnrulyException("Unable to create an instance of [" + type.getName() + "] via Spring.", e);
        }
    }

    /**
     * Handles the ContextClosedEvent by releasing the ApplicationContext and clearing the cache.
     *
     * @param ctxClosedEvent the ContextClosedEvent to be handled
     */
    @EventListener
    public void onContextClosed(ContextClosedEvent ctxClosedEvent) {
        this.ctx = null;
        this.objectCache.clear();
    }

    @Override
    public String toString() {
        return "SpringObjectFactory{" +
                "ctx=" + ctx +
                '}';
    }
}
