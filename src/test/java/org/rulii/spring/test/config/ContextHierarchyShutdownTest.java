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
package org.rulii.spring.test.config;

import org.junit.jupiter.api.Test;
import org.rulii.model.UnrulyException;
import org.rulii.spring.factory.SpringObjectFactory;
import org.rulii.spring.registry.SpringRuleRegistry;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Spring republishes a child context's {@code ContextClosedEvent} to its parent. The registry
 * and object factory must therefore release their bean factory from the owning container's
 * destroy callback, not from a close-event listener - otherwise closing a child (Spring Cloud
 * bootstrap, {@code SpringApplicationBuilder.child()}, {@code @ContextHierarchy}) would shut
 * down the parent's still-running rule infrastructure.
 */
class ContextHierarchyShutdownTest {

    static class Helper {
        public Helper() {
            super();
        }
    }

    @Configuration
    static class ParentConfig {

        @Bean
        SpringRuleRegistry ruleRegistry(ListableBeanFactory beanFactory) {
            return new SpringRuleRegistry(beanFactory);
        }

        @Bean
        SpringObjectFactory objectFactory(BeanFactory beanFactory) {
            return new SpringObjectFactory((AutowireCapableBeanFactory) beanFactory);
        }
    }

    @Configuration
    static class ChildConfig {}

    @Test
    void closingChildContextLeavesParentRegistryAndFactoryUsable() {
        try (AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext(ParentConfig.class)) {
            SpringRuleRegistry registry = parent.getBean(SpringRuleRegistry.class);
            SpringObjectFactory objectFactory = parent.getBean(SpringObjectFactory.class);

            AnnotationConfigApplicationContext child = new AnnotationConfigApplicationContext();
            child.setParent(parent);
            child.register(ChildConfig.class);
            child.refresh();
            child.close();

            assertEquals(0, registry.getCount(), "parent registry must still answer after a child closed");
            assertNotNull(objectFactory.create(Helper.class, false), "parent object factory must still create after a child closed");
        }
    }

    @Test
    void closingOwningContextDisablesRegistryAndFactory() {
        SpringRuleRegistry registry;
        SpringObjectFactory objectFactory;

        try (AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext(ParentConfig.class)) {
            registry = parent.getBean(SpringRuleRegistry.class);
            objectFactory = parent.getBean(SpringObjectFactory.class);
        }

        assertThrows(UnrulyException.class, registry::getCount);
        assertThrows(UnrulyException.class, () -> objectFactory.create(Helper.class, false));
    }
}
