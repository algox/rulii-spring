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
import org.junit.jupiter.api.extension.ExtendWith;
import org.rulii.context.RuleContextOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Verifies that Clock and Locale beans are honored by the auto-configured
 * {@link RuleContextOptions} — e.g. a fixed Clock for deterministic rule tests.
 *
 * <p>Regression test: Clock and Locale were previously hardcoded in the
 * ruleContextOptions factory and could not be overridden without replacing the
 * entire options bean.
 *
 * @author Max Arulananthan
 * @since 2.0
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = ClockLocaleOverrideTest.Config.class)
class ClockLocaleOverrideTest {

    static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    // Plain @Configuration (not @SpringBootConfiguration) so other tests in this package
    // using default configuration resolution do not pick this class up.
    @Configuration
    @EnableAutoConfiguration
    static class Config {

        @Bean
        public Clock ruleClock() {
            return FIXED_CLOCK;
        }

        @Bean
        public Locale ruleLocale() {
            return Locale.CANADA_FRENCH;
        }
    }

    @Autowired
    private RuleContextOptions options;

    @Test
    void clockAndLocaleBeansAreHonored() {
        assertSame(FIXED_CLOCK, options.getClock(), "a Clock bean must override the system default");
        assertEquals(Locale.CANADA_FRENCH, options.getLocale(), "a Locale bean must override the JVM default");
    }
}
