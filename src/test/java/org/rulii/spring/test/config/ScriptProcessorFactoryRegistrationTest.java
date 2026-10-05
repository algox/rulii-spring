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
import org.rulii.script.ScriptProcessorFactory;
import org.rulii.script.ScriptProcessorManager;
import org.rulii.spring.config.BeanNames;
import org.rulii.spring.config.ScriptProcessorFactoryRegistration;
import org.rulii.spring.script.el.SpelScriptProcessorFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The {@link ScriptProcessorManager} is JVM-global. Factory beans registered into it must be
 * taken out again when their context closes, or the closed context stays reachable through
 * them for the life of the process.
 */
class ScriptProcessorFactoryRegistrationTest {

    static final String CUSTOM_LANGUAGE = "registration-test-lang";

    /** A factory under a language nobody else uses; also a stand-in for one with injected state. */
    static class CustomLanguageFactory extends SpelScriptProcessorFactory {
        CustomLanguageFactory() {
            super(CUSTOM_LANGUAGE, "ctx");
        }
    }

    /** Overrides the stock {@code el} factory. */
    static class CustomElFactory extends SpelScriptProcessorFactory {
        CustomElFactory() {
            super();
        }
    }

    // Plain @Configuration (not @SpringBootConfiguration) so other tests in this package
    // using default configuration resolution do not pick this class up.
    @Configuration
    @EnableAutoConfiguration
    static class CustomLanguageConfig {
        @Bean
        ScriptProcessorFactory customLanguageFactory() {
            return new CustomLanguageFactory();
        }
    }

    @Configuration
    @EnableAutoConfiguration
    static class CustomElConfig {
        @Bean
        ScriptProcessorFactory customElFactory() {
            return new CustomElFactory();
        }
    }

    @Test
    void factoryBeanIsRegisteredWhileOpenAndRemovedOnClose() {
        ScriptProcessorManager manager = ScriptProcessorManager.getInstance();
        ScriptProcessorFactory factory;

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(CustomLanguageConfig.class)) {
            factory = context.getBean("customLanguageFactory", ScriptProcessorFactory.class);
            assertSame(factory, manager.getScriptProcessorFactory(CUSTOM_LANGUAGE));
            assertEquals(1, context.getBean(ScriptProcessorFactoryRegistration.class).getRegistered().size());
            assertTrue(context.containsBean(BeanNames.SCRIPT_MANAGER), "the manager bean is still exposed");
        }

        assertNull(manager.getScriptProcessorFactory(CUSTOM_LANGUAGE),
                "a closed context's factory must not stay in the JVM-wide registry");
    }

    @Test
    void closingContextRestoresStockFactoryForOverriddenLanguage() {
        ScriptProcessorManager manager = ScriptProcessorManager.getInstance();
        ScriptProcessorFactory override;

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(CustomElConfig.class)) {
            override = context.getBean("customElFactory", ScriptProcessorFactory.class);
            assertSame(override, manager.getScriptProcessorFactory(SpelScriptProcessorFactory.LANGUAGE_NAME));
        }

        ScriptProcessorFactory restored = manager.getScriptProcessorFactory(SpelScriptProcessorFactory.LANGUAGE_NAME);
        assertNotNull(restored, "el must still be usable by later contexts");
        assertNotSame(override, restored);
        assertEquals(SpelScriptProcessorFactory.class, restored.getClass(), "the ServiceLoader default must be back");
    }

    @Test
    void newerContextsRegistrationSurvivesOlderContextClosing() {
        ScriptProcessorManager manager = ScriptProcessorManager.getInstance();

        AnnotationConfigApplicationContext older = new AnnotationConfigApplicationContext(CustomLanguageConfig.class);
        try (AnnotationConfigApplicationContext newer = new AnnotationConfigApplicationContext(CustomLanguageConfig.class)) {
            ScriptProcessorFactory newerFactory = newer.getBean("customLanguageFactory", ScriptProcessorFactory.class);
            assertSame(newerFactory, manager.getScriptProcessorFactory(CUSTOM_LANGUAGE));

            older.close();

            assertSame(newerFactory, manager.getScriptProcessorFactory(CUSTOM_LANGUAGE),
                    "removal is by identity: the older context must not clobber the newer registration");
        }

        assertNull(manager.getScriptProcessorFactory(CUSTOM_LANGUAGE));
    }
}
