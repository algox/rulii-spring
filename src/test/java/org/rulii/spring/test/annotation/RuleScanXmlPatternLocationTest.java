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
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies that {@link RuleScan#xmlLocations()} accepts a user-supplied resource pattern
 * (containing {@code *}) and uses it as-is for resolution.
 *
 * <p>Regression test: a pattern location previously had {@code /*.xml} appended, producing
 * a pattern that matched nothing and silently registered no rules.
 *
 * @author Max Arulananthan
 * @since 2.0
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = RuleScanXmlPatternLocationTest.Config.class)
class RuleScanXmlPatternLocationTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @RuleScan(xmlLocations = "classpath*:rules/xml-scan/*.xml")
    static class Config {}

    @Autowired
    private RuleRegistry ruleRegistry;

    @Test
    void rulesFromPatternLocationAreRegistered() {
        assertNotNull(ruleRegistry.getRule("XmlDeclaredAgeRule"));
        assertNotNull(ruleRegistry.getRule("XmlDeclaredPositiveRule"));
        assertNotNull(ruleRegistry.getRuleSet("XmlDeclaredRuleSet"));
    }
}
