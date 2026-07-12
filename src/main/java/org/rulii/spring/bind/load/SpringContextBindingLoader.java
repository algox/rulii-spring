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
package org.rulii.spring.bind.load;

import org.rulii.bind.Binding;
import org.rulii.bind.Bindings;
import org.rulii.bind.load.BindingLoader;
import org.rulii.util.RuleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.util.Assert;

import java.util.function.Supplier;

/**
 * A class that implements BindingLoader interface to load bindings from a Spring application context.
 * It binds beans from the given ListableBeanFactory into the provided Bindings object.
 *
 * <p>Bindings are lazy: bean types are resolved from bean-definition metadata (no bean is
 * instantiated by loading), and each binding's value is fetched from the factory on access.
 * Bean names that are not valid rulii binding names (e.g. Spring-internal dotted names such
 * as {@code org.springframework.context.annotation.internalConfigurationAnnotationProcessor})
 * are skipped, as are definitions whose type cannot be determined without instantiation.
 *
 * @author Max Arulananthan
 * @since 1.0
 */
public class SpringContextBindingLoader implements BindingLoader<ListableBeanFactory> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringContextBindingLoader.class);

    public SpringContextBindingLoader() {
        super();
    }

    /**
     * Loads bindable beans from the given ListableBeanFactory into the provided Bindings object.
     *
     * @param bindings the Bindings object to bind the beans into
     * @param factory the ListableBeanFactory containing the beans to load
     */
    @Override
    public void load(Bindings bindings, ListableBeanFactory factory) {
        Assert.notNull(bindings, "bindings cannot be null.");
        Assert.notNull(factory, "factory cannot be null.");

        LOGGER.debug("Loading Spring Context as Bindings.");

        for (String beanName : factory.getBeanDefinitionNames()) {
            if (!RuleUtils.isValidName(beanName)) {
                LOGGER.debug("Skipping bean [{}]: not a valid binding name.", beanName);
                continue;
            }

            Class<?> type;
            try {
                // Metadata-only type resolution - never instantiates the bean.
                type = factory.getType(beanName);
            } catch (BeansException e) {
                LOGGER.debug("Skipping bean [{}]: type cannot be determined ({}).", beanName, e.getMessage());
                continue;
            }

            if (type == null) {
                LOGGER.debug("Skipping bean [{}]: type cannot be determined without instantiation.", beanName);
                continue;
            }

            Supplier<Object> getter = () -> factory.getBean(beanName);
            // Bind the property
            bindings.bind(Binding.builder().with(beanName)
                    .type(type)
                    .delegate(getter, null)
                    .editable(false)
                    .build());
        }
    }
}
