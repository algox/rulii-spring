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
package org.rulii.spring.test.context;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rulii.bind.match.BindingMatchingStrategy;
import org.rulii.bind.match.ParameterResolver;
import org.rulii.convert.ConverterRegistry;
import org.rulii.spring.context.SpringEnabledRuleContextOptions;
import org.rulii.text.MessageFormatter;
import org.rulii.text.MessageResolver;
import org.rulii.util.reflect.ObjectFactory;

import java.time.Clock;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class SpringEnabledRuleContextOptionsTest {

    @Mock private BindingMatchingStrategy matchingStrategy;
    @Mock private ParameterResolver parameterResolver;
    @Mock private MessageFormatter messageFormatter;
    @Mock private ConverterRegistry converterRegistry;
    @Mock private ObjectFactory objectFactory;
    @Mock private MessageResolver messageResolver;

    private SpringEnabledRuleContextOptions buildOptions() {
        return new SpringEnabledRuleContextOptions(
                matchingStrategy, parameterResolver, messageFormatter,
                converterRegistry, objectFactory, messageResolver,
                Executors.newSingleThreadExecutor(), Clock.systemDefaultZone(), Locale.ENGLISH);
    }

    @Test
    public void testGetMatchingStrategy() {
        assertEquals(matchingStrategy, buildOptions().getMatchingStrategy());
    }

    @Test
    public void testGetParameterResolver() {
        assertEquals(parameterResolver, buildOptions().getParameterResolver());
    }

    @Test
    public void testGetMessageFormatter() {
        assertEquals(messageFormatter, buildOptions().getMessageFormatter());
    }

    @Test
    public void testGetConverterRegistry() {
        assertEquals(converterRegistry, buildOptions().getConverterRegistry());
    }

    @Test
    public void testGetObjectFactory() {
        assertEquals(objectFactory, buildOptions().getObjectFactory());
    }

    @Test
    public void testGetMessageResolver() {
        assertEquals(messageResolver, buildOptions().getMessageResolver());
    }

    @Test
    public void testGetClock() {
        Clock clock = Clock.systemUTC();
        SpringEnabledRuleContextOptions options = new SpringEnabledRuleContextOptions(
                matchingStrategy, parameterResolver, messageFormatter,
                converterRegistry, objectFactory, messageResolver,
                Executors.newSingleThreadExecutor(), clock, Locale.ENGLISH);
        assertEquals(clock, options.getClock());
    }

    @Test
    public void testGetLocale() {
        SpringEnabledRuleContextOptions options = new SpringEnabledRuleContextOptions(
                matchingStrategy, parameterResolver, messageFormatter,
                converterRegistry, objectFactory, messageResolver,
                Executors.newSingleThreadExecutor(), Clock.systemDefaultZone(), Locale.FRANCE);
        assertEquals(Locale.FRANCE, options.getLocale());
    }

    @Test
    public void testGetExecutorService() {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        SpringEnabledRuleContextOptions options = new SpringEnabledRuleContextOptions(
                matchingStrategy, parameterResolver, messageFormatter,
                converterRegistry, objectFactory, messageResolver,
                executorService, Clock.systemDefaultZone(), Locale.ENGLISH);
        assertEquals(executorService, options.getExecutorService());
    }

    @Test
    public void testToString() {
        String str = buildOptions().toString();
        assertNotNull(str);
        assertTrue(str.contains("SpringEnabledRuleContextOptions"));
    }

    @Test
    public void testNullMatchingStrategyThrows() {
        assertThrows(Exception.class, () -> new SpringEnabledRuleContextOptions(
                null, parameterResolver, messageFormatter,
                converterRegistry, objectFactory, messageResolver,
                Executors.newSingleThreadExecutor(), Clock.systemDefaultZone(), Locale.ENGLISH));
    }

    @Test
    public void testNullParameterResolverThrows() {
        assertThrows(Exception.class, () -> new SpringEnabledRuleContextOptions(
                matchingStrategy, null, messageFormatter,
                converterRegistry, objectFactory, messageResolver,
                Executors.newSingleThreadExecutor(), Clock.systemDefaultZone(), Locale.ENGLISH));
    }

    @Test
    public void testNullMessageFormatterThrows() {
        assertThrows(Exception.class, () -> new SpringEnabledRuleContextOptions(
                matchingStrategy, parameterResolver, null,
                converterRegistry, objectFactory, messageResolver,
                Executors.newSingleThreadExecutor(), Clock.systemDefaultZone(), Locale.ENGLISH));
    }

    @Test
    public void testNullConverterRegistryThrows() {
        assertThrows(Exception.class, () -> new SpringEnabledRuleContextOptions(
                matchingStrategy, parameterResolver, messageFormatter,
                null, objectFactory, messageResolver,
                Executors.newSingleThreadExecutor(), Clock.systemDefaultZone(), Locale.ENGLISH));
    }

    @Test
    public void testNullObjectFactoryThrows() {
        assertThrows(Exception.class, () -> new SpringEnabledRuleContextOptions(
                matchingStrategy, parameterResolver, messageFormatter,
                converterRegistry, null, messageResolver,
                Executors.newSingleThreadExecutor(), Clock.systemDefaultZone(), Locale.ENGLISH));
    }

    @Test
    public void testNullMessageResolverThrows() {
        assertThrows(Exception.class, () -> new SpringEnabledRuleContextOptions(
                matchingStrategy, parameterResolver, messageFormatter,
                converterRegistry, objectFactory, null,
                Executors.newSingleThreadExecutor(), Clock.systemDefaultZone(), Locale.ENGLISH));
    }

    @Test
    public void testNullExecutorServiceThrows() {
        assertThrows(Exception.class, () -> new SpringEnabledRuleContextOptions(
                matchingStrategy, parameterResolver, messageFormatter,
                converterRegistry, objectFactory, messageResolver,
                null, Clock.systemDefaultZone(), Locale.ENGLISH));
    }

    @Test
    public void testNullClockThrows() {
        assertThrows(Exception.class, () -> new SpringEnabledRuleContextOptions(
                matchingStrategy, parameterResolver, messageFormatter,
                converterRegistry, objectFactory, messageResolver,
                Executors.newSingleThreadExecutor(), null, Locale.ENGLISH));
    }

    @Test
    public void testNullLocaleThrows() {
        assertThrows(Exception.class, () -> new SpringEnabledRuleContextOptions(
                matchingStrategy, parameterResolver, messageFormatter,
                converterRegistry, objectFactory, messageResolver,
                Executors.newSingleThreadExecutor(), Clock.systemDefaultZone(), null));
    }
}
