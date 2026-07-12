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
package org.rulii.spring.test.bind.load;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rulii.bind.Bindings;
import org.rulii.spring.bind.load.SpringContextBindingLoader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link SpringContextBindingLoader} against a REAL application context.
 *
 * <p>Regression tests: the loader previously bound raw Spring bean names (dotted internal
 * names violate rulii's binding-name rules and aborted the whole load) and eagerly
 * instantiated every bean just to read its class, defeating its own lazy delegate.
 *
 * @author Max Arulananthan
 * @since 1.0
 */
public class SpringContextBindingLoaderTest {

    static final AtomicBoolean LAZY_CREATED = new AtomicBoolean(false);

    /** Expensive bean that must NOT be instantiated by loading bindings. */
    static class ExpensiveBean {
        ExpensiveBean() {
            super();
            LAZY_CREATED.set(true);
        }
    }

    @Configuration
    static class Config {

        @Bean
        public String myBean() {
            return "beanValue";
        }

        @Bean
        public Integer intBean() {
            return 100;
        }

        @Bean
        @Lazy
        public ExpensiveBean expensiveBean() {
            return new ExpensiveBean();
        }
    }

    private AnnotationConfigApplicationContext context;
    private final SpringContextBindingLoader loader = new SpringContextBindingLoader();

    @BeforeEach
    public void setUp() {
        LAZY_CREATED.set(false);
        context = new AnnotationConfigApplicationContext(Config.class);
    }

    @AfterEach
    public void tearDown() {
        context.close();
    }

    private Bindings load() {
        Bindings bindings = Bindings.builder().standard();
        loader.load(bindings, context.getBeanFactory());
        return bindings;
    }

    @Test
    public void testLoadBindsBeansWithValuesAndTypes() {
        Bindings bindings = load();

        assertTrue(bindings.contains("myBean"));
        assertEquals("beanValue", bindings.getValue("myBean"));
        assertTrue(bindings.contains("intBean"));
        assertEquals(Integer.class, bindings.getBinding("intBean").getType());
    }

    @Test
    public void testLoadBindingsAreReadOnly() {
        Bindings bindings = load();
        assertFalse(bindings.getBinding("myBean").isEditable());
    }

    @Test
    public void testDottedInternalBeanNamesAreSkippedNotFatal() {
        // A real annotation-config context always contains dotted internal names.
        assertTrue(Arrays.stream(context.getBeanFactory().getBeanDefinitionNames())
                        .anyMatch(name -> name.contains(".")),
                "precondition: the context should contain dotted bean names");

        Bindings bindings = load();

        assertTrue(bindings.contains("myBean"), "valid beans must still be bound");
        assertTrue(bindings.size() > 0);
    }

    @Test
    public void testLoadDoesNotInstantiateLazyBeans() {
        Bindings bindings = load();

        assertFalse(LAZY_CREATED.get(), "loading bindings must not instantiate @Lazy beans");
        assertTrue(bindings.contains("expensiveBean"), "the lazy bean must still be bound (by metadata type)");
        assertEquals(ExpensiveBean.class, bindings.getBinding("expensiveBean").getType());

        // Accessing the binding value resolves the bean lazily.
        assertNotNull(bindings.getValue("expensiveBean"));
        assertTrue(LAZY_CREATED.get());
    }

    @Test
    public void testLoadNullBindingsThrows() {
        assertThrows(Exception.class, () -> loader.load(null, context.getBeanFactory()));
    }

    @Test
    public void testLoadNullFactoryThrows() {
        assertThrows(Exception.class, () -> loader.load(Bindings.builder().standard(), null));
    }
}
