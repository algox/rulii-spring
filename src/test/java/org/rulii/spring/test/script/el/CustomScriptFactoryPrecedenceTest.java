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
package org.rulii.spring.test.script.el;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.rulii.script.ScriptProcessorFactory;
import org.rulii.script.ScriptProcessorManager;
import org.rulii.spring.script.el.SpelScriptProcessorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Verifies that a user-defined {@link ScriptProcessorFactory} bean takes precedence over
 * the ServiceLoader-discovered default for the same language.
 *
 * <p>Regression test: {@code RuleConfig.scriptProcessorManager} registered user beans into
 * the global manager BEFORE its lazy ServiceLoader discovery had run; the first rule
 * evaluation then triggered {@code load()}, which unconditionally re-registered the
 * ServiceLoader default over the user's factory — making the winner dependent on bean
 * instantiation order.
 *
 * @author Max Arulananthan
 * @since 2.0
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = CustomScriptFactoryPrecedenceTest.Config.class)
class CustomScriptFactoryPrecedenceTest {

    /** Marker subclass so the test can assert identity, behaviorally identical to the default. */
    static class CustomSpelFactory extends SpelScriptProcessorFactory {
        CustomSpelFactory() {
            super();
        }
    }

    // Plain @Configuration (not @SpringBootConfiguration) so other tests in this package
    // using default configuration resolution do not pick this class up.
    @Configuration
    @EnableAutoConfiguration
    static class Config {

        @Bean
        public ScriptProcessorFactory customElFactory() {
            return new CustomSpelFactory();
        }
    }

    @Autowired
    private ScriptProcessorFactory customElFactory;

    @AfterAll
    static void restoreDefaultFactory() {
        // The manager is a JVM-wide singleton; put the stock factory back so this test
        // cannot leak its marker factory into other tests in the same JVM.
        ScriptProcessorManager.getInstance().register(new SpelScriptProcessorFactory());
    }

    @Test
    void userFactoryBeanWinsOverServiceLoaderDefault() {
        ScriptProcessorFactory active = ScriptProcessorManager.getInstance()
                .getScriptProcessorFactory(SpelScriptProcessorFactory.LANGUAGE_NAME);

        assertSame(customElFactory, active,
                "the user's factory bean must not be clobbered by lazy ServiceLoader discovery");
    }
}
