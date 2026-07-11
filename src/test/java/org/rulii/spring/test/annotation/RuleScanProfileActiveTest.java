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
package org.rulii.spring.test.annotation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.rulii.registry.RuleRegistry;
import org.rulii.rule.Rule;
import org.rulii.spring.annotation.RuleScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that {@link RuleScan} classpath scanning honors {@code @Profile} on rule
 * classes when the profile is <em>active</em>: both the unprofiled rule and the
 * profile-scoped rule must be registered.
 *
 * <p>Regression test: the scanner previously evaluated {@code @Profile} against a
 * throwaway {@code Environment}, ignoring the running application's active profiles.
 *
 * @author Max Arulananthan
 * @since 2.0
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = RuleScanProfileActiveTest.Config.class)
@ActiveProfiles("rule-profile")
class RuleScanProfileActiveTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @RuleScan(scanBasePackages = "org.rulii.spring.test.rules.profiletest")
    static class Config {}

    @Autowired
    private ApplicationContext context;

    @Autowired
    private RuleRegistry ruleRegistry;

    @Test
    void unprofiledRuleIsRegistered() {
        assertTrue(context.containsBean("AlwaysActiveRule"));
        assertNotNull(ruleRegistry.getRule("AlwaysActiveRule"));
    }

    @Test
    void profileScopedRuleIsRegisteredWhenProfileIsActive() {
        assertTrue(context.containsBean("ProfileScopedRule"));
        Rule rule = ruleRegistry.getRule("ProfileScopedRule");
        assertNotNull(rule, "ProfileScopedRule must be registered when 'rule-profile' is active");
    }
}
