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
package org.rulii.spring.test.factory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rulii.model.UnrulyException;
import org.rulii.spring.factory.SpringObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SpringObjectFactory}.
 *
 * <p>Regression tests: the factory previously ignored {@code isUseCache} (rebuilding
 * stateless helpers through the full bean-creation chain on every rule execution),
 * leaked Spring {@code BeansException} where the ObjectFactory contract documents
 * {@code UnrulyException}, and threw {@code IllegalArgumentException} for a closed context.
 *
 * @author Max Arulananthan
 * @since 1.0
 */
public class SpringObjectFactoryTest {

    static class SimpleHelper {
        public SimpleHelper() {
            super();
        }
    }

    interface MissingDependency { }

    static class Unsatisfiable {
        @SuppressWarnings("unused")
        public Unsatisfiable(@Autowired MissingDependency dependency) {
            super();
        }
    }

    @Configuration
    static class Config { }

    private AnnotationConfigApplicationContext context;
    private SpringObjectFactory factory;

    @BeforeEach
    public void setUp() {
        context = new AnnotationConfigApplicationContext(Config.class);
        factory = new SpringObjectFactory(context.getAutowireCapableBeanFactory());
    }

    @AfterEach
    public void tearDown() {
        context.close();
    }

    @Test
    public void testCreateWithCacheReturnsSameInstance() {
        SimpleHelper first = factory.create(SimpleHelper.class, true);
        SimpleHelper second = factory.create(SimpleHelper.class, true);
        assertSame(first, second, "isUseCache=true must reuse the created instance");
    }

    @Test
    public void testCreateWithoutCacheReturnsNewInstances() {
        SimpleHelper first = factory.create(SimpleHelper.class, false);
        SimpleHelper second = factory.create(SimpleHelper.class, false);
        assertNotSame(first, second);
    }

    @Test
    public void testSpringFailureIsWrappedInUnrulyException() {
        UnrulyException ex = assertThrows(UnrulyException.class,
                () -> factory.create(Unsatisfiable.class, false));
        assertNotNull(ex.getCause(), "the Spring cause must be preserved");
        assertTrue(ex.getMessage().contains(Unsatisfiable.class.getName()));
    }

    @Test
    public void testClosedContextThrowsUnrulyException() {
        factory.onContextClosed(mock(ContextClosedEvent.class));
        assertThrows(UnrulyException.class, () -> factory.create(SimpleHelper.class, false));
    }
}
