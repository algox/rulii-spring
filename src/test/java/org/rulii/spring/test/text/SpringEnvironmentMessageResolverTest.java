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
package org.rulii.spring.test.text;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rulii.spring.text.SpringEnvironmentMessageResolver;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.env.Environment;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SpringEnvironmentMessageResolverTest {

    @Mock
    private Environment environment;

    @Test
    public void testResolveExistingCode() {
        when(environment.getProperty("error.100", "default")).thenReturn("Found message");
        SpringEnvironmentMessageResolver resolver = new SpringEnvironmentMessageResolver(environment);
        String result = resolver.resolve(Locale.ENGLISH, "error.100", "default");
        assertEquals("Found message", result);
    }

    @Test
    public void testResolveMissingCodeReturnsDefault() {
        when(environment.getProperty("unknown.code", "fallback")).thenReturn("fallback");
        SpringEnvironmentMessageResolver resolver = new SpringEnvironmentMessageResolver(environment);
        String result = resolver.resolve(Locale.ENGLISH, "unknown.code", "fallback");
        assertEquals("fallback", result);
    }

    @Test
    public void testLocaleIsIgnoredForResolution() {
        when(environment.getProperty("msg.1", "default")).thenReturn("Hello");
        SpringEnvironmentMessageResolver resolver = new SpringEnvironmentMessageResolver(environment);
        String resultFrench = resolver.resolve(Locale.FRENCH, "msg.1", "default");
        String resultGerman = resolver.resolve(Locale.GERMAN, "msg.1", "default");
        assertEquals("Hello", resultFrench);
        assertEquals("Hello", resultGerman);
        verify(environment, times(2)).getProperty("msg.1", "default");
    }

    @Test
    public void testResolveNullDefaultMessage() {
        when(environment.getProperty("some.code", (String) null)).thenReturn(null);
        SpringEnvironmentMessageResolver resolver = new SpringEnvironmentMessageResolver(environment);
        assertNull(resolver.resolve(Locale.ENGLISH, "some.code", null));
    }

    @Test
    public void testNullEnvironmentThrows() {
        assertThrows(Exception.class, () -> new SpringEnvironmentMessageResolver(null));
    }

    @Test
    public void testResolveDelegatesToEnvironment() {
        when(environment.getProperty("test.key", "fallback")).thenReturn("value");
        SpringEnvironmentMessageResolver resolver = new SpringEnvironmentMessageResolver(environment);
        resolver.resolve(Locale.ENGLISH, "test.key", "fallback");
        verify(environment).getProperty("test.key", "fallback");
    }

    @Test
    public void testMessageSourceTakesPrecedenceAndIsLocaleAware() {
        StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.addMessage("greeting", Locale.ENGLISH, "Hello");
        messageSource.addMessage("greeting", Locale.FRENCH, "Bonjour");

        SpringEnvironmentMessageResolver resolver = new SpringEnvironmentMessageResolver(environment, messageSource);

        assertEquals("Hello", resolver.resolve(Locale.ENGLISH, "greeting", "default"));
        assertEquals("Bonjour", resolver.resolve(Locale.FRENCH, "greeting", "default"));
        verifyNoInteractions(environment);
    }

    @Test
    public void testEnvironmentFallbackWhenMessageSourceMisses() {
        StaticMessageSource messageSource = new StaticMessageSource();
        when(environment.getProperty("env.only", "default")).thenReturn("from-env");

        SpringEnvironmentMessageResolver resolver = new SpringEnvironmentMessageResolver(environment, messageSource);

        assertEquals("from-env", resolver.resolve(Locale.ENGLISH, "env.only", "default"));
    }

    @Test
    public void testNullCodeReturnsDefaultMessage() {
        SpringEnvironmentMessageResolver resolver = new SpringEnvironmentMessageResolver(environment);
        assertEquals("fallback", resolver.resolve(Locale.ENGLISH, null, "fallback"));
        verifyNoInteractions(environment);
    }

    @Test
    public void testUnresolvablePlaceholderFallsBackToDefault() {
        when(environment.getProperty("bad.placeholder", "default"))
                .thenThrow(new IllegalArgumentException("Could not resolve placeholder 'min'"));

        SpringEnvironmentMessageResolver resolver = new SpringEnvironmentMessageResolver(environment);

        assertEquals("default", resolver.resolve(Locale.ENGLISH, "bad.placeholder", "default"));
    }

    @Test
    public void testNullLocaleUsesJvmDefaultForMessageSource() {
        StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.addMessage("code", Locale.getDefault(), "resolved");

        SpringEnvironmentMessageResolver resolver = new SpringEnvironmentMessageResolver(environment, messageSource);

        assertEquals("resolved", resolver.resolve(null, "code", "default"));
    }
}
