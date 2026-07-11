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
import org.rulii.spring.config.RuleRegistrarMetaInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that multiple {@code @RuleScan} configuration classes coexist: rules from
 * every scan are registered and their meta-info is merged into a single injectable
 * {@link RuleRegistrarMetaInfo} bean.
 *
 * <p>Regression test: each {@code @RuleScan} previously registered its meta-info under
 * the same generated bean name, so a second {@code @RuleScan} failed startup with
 * {@code BeanDefinitionOverrideException}.
 *
 * @author Max Arulananthan
 * @since 2.0
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {RuleScanMultipleConfigsTest.ConfigA.class, RuleScanMultipleConfigsTest.ConfigB.class})
class RuleScanMultipleConfigsTest {

    @Configuration
    @EnableAutoConfiguration
    @RuleScan(scanBasePackages = "org.rulii.spring.test.rules.xmlscan")
    static class ConfigA {}

    @Configuration
    @RuleScan(scanBasePackages = "org.rulii.spring.test.rules.profiletest")
    static class ConfigB {}

    @Autowired
    private ApplicationContext context;

    @Autowired
    private RuleRegistry ruleRegistry;

    @Autowired
    private RuleRegistrarMetaInfo metaInfo;

    @Test
    void rulesFromBothScansAreRegistered() {
        assertNotNull(ruleRegistry.getRule("XmlScanRule1"));
        assertNotNull(ruleRegistry.getRule("XmlScanRule2"));
        assertNotNull(ruleRegistry.getRule("AlwaysActiveRule"));
    }

    @Test
    void singleMetaInfoBeanExists() {
        assertEquals(1, context.getBeansOfType(RuleRegistrarMetaInfo.class).size(),
                "Multiple @RuleScan classes must yield exactly one merged meta-info bean");
    }

    @Test
    void metaInfoMergesBothScans() {
        List<String> packages = metaInfo.rulePackages();
        assertTrue(packages.contains("org.rulii.spring.test.rules.xmlscan"));
        assertTrue(packages.contains("org.rulii.spring.test.rules.profiletest"));
        assertTrue(metaInfo.xmlLocations().isEmpty());
        // 2 rules from xmlscan + 1 from profiletest (ProfileScopedRule excluded - profile inactive)
        assertEquals(3, metaInfo.ruleCount());
    }
}
