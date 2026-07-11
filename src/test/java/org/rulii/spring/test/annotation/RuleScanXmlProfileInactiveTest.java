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
 * Verifies that XML rule contexts loaded via {@link RuleScan#xmlLocations()} honor
 * {@code <beans profile="...">} sections when the profile is <em>not</em> active: the
 * profile-scoped rule must be skipped while the unprofiled rule is still registered.
 *
 * @author Max Arulananthan
 * @since 2.0
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = RuleScanXmlProfileInactiveTest.Config.class)
class RuleScanXmlProfileInactiveTest {

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
    void profileScopedXmlRuleIsSkippedWhenProfileIsInactive() {
        assertFalse(context.containsBean("XmlProfileScopedRule"),
                "XmlProfileScopedRule must not be registered when 'xml-profile' is inactive");
        assertNull(ruleRegistry.getRule("XmlProfileScopedRule"));
    }
}
