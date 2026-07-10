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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.rulii.bind.match.BindingMatchingStrategy;
import org.rulii.convert.Converter;
import org.rulii.registry.RuleRegistry;
import org.rulii.spring.factory.SpringObjectFactory;
import org.rulii.spring.test.rules.seta.TestRule3;
import org.rulii.util.reflect.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Verifies that an application-supplied ObjectFactory bean — registered under a custom
 * bean name — is honored by @RuleScan rule registration. Scanned rule bean definitions
 * must resolve the ObjectFactory by type, not by the auto-configured bean name.
 *
 * @author Max Arulananthan
 * @since 2.0
 */
@SpringBootTest
public class CustomObjectFactoryOverrideTest {

    @Autowired
    private RuleRegistry ruleRegistry;
    @Autowired
    private ObjectFactory objectFactory;
    @Autowired
    private RecordingObjectFactory recordingObjectFactory;

    public CustomObjectFactoryOverrideTest() {
        super();
    }

    @Test
    public void testCustomObjectFactoryBeanSuppressesAutoConfiguredOne() {
        Assertions.assertInstanceOf(RecordingObjectFactory.class, objectFactory);
    }

    @Test
    public void testScannedRulesAreBuiltWithCustomObjectFactory() {
        Assertions.assertNotNull(ruleRegistry.getRule(TestRule3.class));
        Assertions.assertTrue(recordingObjectFactory.getRuleCreationCount() > 0,
                "scanned rules must be constructed through the application-supplied ObjectFactory");
    }

    @TestConfiguration
    static class Config {

        public Config() {
            super();
        }

        @Bean
        public RecordingObjectFactory myCustomObjectFactory(AutowireCapableBeanFactory beanFactory) {
            return new RecordingObjectFactory(new SpringObjectFactory(beanFactory));
        }
    }

    /**
     * Delegating ObjectFactory that records how many rule instances it created.
     */
    static class RecordingObjectFactory implements ObjectFactory {

        private final ObjectFactory delegate;
        private final AtomicInteger ruleCreationCount = new AtomicInteger();

        RecordingObjectFactory(ObjectFactory delegate) {
            super();
            this.delegate = delegate;
        }

        @Override
        public <T extends BindingMatchingStrategy> T createBindingMatchingStrategy(Class<T> type) {
            return delegate.createBindingMatchingStrategy(type);
        }

        @Override
        public <T> T createAction(Class<T> type) {
            return delegate.createAction(type);
        }

        @Override
        public <T> T createCondition(Class<T> type) {
            return delegate.createCondition(type);
        }

        @Override
        public <T> T createFunction(Class<T> type) {
            return delegate.createFunction(type);
        }

        @Override
        public <T, R> Converter<T, R> createConverter(Class<? extends Converter<T, R>> type) {
            return delegate.createConverter(type);
        }

        @Override
        public <T> T createRule(Class<T> type) {
            ruleCreationCount.incrementAndGet();
            return delegate.createRule(type);
        }

        @Override
        public <T> T create(Class<T> type, boolean isUseCache) {
            return delegate.create(type, isUseCache);
        }

        public int getRuleCreationCount() {
            return ruleCreationCount.get();
        }
    }
}
