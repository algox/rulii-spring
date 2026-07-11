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
 * Verifies that XML rule contexts loaded via {@link RuleScan#xmlLocations()} honor
 * {@code <beans profile="...">} sections when the profile is <em>active</em>: both the
 * unprofiled rule and the profile-scoped rule must be registered.
 *
 * <p>Regression test: the {@code XmlBeanDefinitionReader} previously fell back to a
 * fresh {@code StandardEnvironment}, silently skipping profile-gated sections.
 *
 * @author Max Arulananthan
 * @since 2.0
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = RuleScanXmlProfileActiveTest.Config.class)
@ActiveProfiles("xml-profile")
class RuleScanXmlProfileActiveTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @RuleScan(xmlLocations = "classpath:rules/xml-profile/")
    static class Config {}

    @Autowired
    private ApplicationContext context;

    @Autowired
    private RuleRegistry ruleRegistry;

    @Test
    void unprofiledXmlRuleIsRegistered() {
        assertTrue(context.containsBean("XmlAlwaysActiveRule"));
        assertNotNull(ruleRegistry.getRule("XmlAlwaysActiveRule"));
    }

    @Test
    void profileScopedXmlRuleIsRegisteredWhenProfileIsActive() {
        assertTrue(context.containsBean("XmlProfileScopedRule"));
        Rule rule = ruleRegistry.getRule("XmlProfileScopedRule");
        assertNotNull(rule, "XmlProfileScopedRule must be registered when 'xml-profile' is active");
    }
}
