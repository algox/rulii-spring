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
import org.rulii.spring.annotation.RuleScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that {@link RuleScan} classpath scanning honors {@code @Profile} on rule
 * classes when the profile is <em>not</em> active: the profile-scoped rule must be
 * skipped while the unprofiled rule is still registered.
 *
 * @author Max Arulananthan
 * @since 2.0
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = RuleScanProfileInactiveTest.Config.class)
class RuleScanProfileInactiveTest {

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
    void profileScopedRuleIsSkippedWhenProfileIsInactive() {
        assertFalse(context.containsBean("ProfileScopedRule"),
                "ProfileScopedRule must not be registered when 'rule-profile' is inactive");
        assertNull(ruleRegistry.getRule("ProfileScopedRule"));
    }
}
