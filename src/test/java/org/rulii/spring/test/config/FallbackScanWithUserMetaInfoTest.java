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
import org.rulii.spring.config.BeanNames;
import org.rulii.spring.config.RuleBeanDefinitionRegistryPostProcessor;
import org.rulii.spring.config.RuleRegistrarMetaInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that an application-defined {@link RuleRegistrarMetaInfo} bean does not
 * disable the auto-scan fallback.
 *
 * <p>Regression test: the fallback was previously guarded by
 * {@code @ConditionalOnMissingBean(RuleRegistrarMetaInfo.class)}, so any user-defined
 * meta-info bean silently switched rule scanning off. The condition now targets the
 * internal marker registered only by {@code @RuleScan} processing.
 *
 * @author Max Arulananthan
 * @since 2.0
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = FallbackScanWithUserMetaInfoTest.Config.class)
class FallbackScanWithUserMetaInfoTest {

    // Plain @Configuration (not @SpringBootConfiguration) so other tests in this package
    // using default configuration resolution do not pick this class up.
    @Configuration
    @EnableAutoConfiguration
    static class Config {

        @Bean
        public RuleRegistrarMetaInfo customMetaInfo() {
            return new RuleRegistrarMetaInfo(List.of(), List.of(), 0);
        }
    }

    @Autowired
    private ApplicationContext context;

    @Test
    void userDefinedMetaInfoDoesNotDisableAutoScan() {
        assertNotNull(context.getBean(RuleBeanDefinitionRegistryPostProcessor.class),
                "Auto-scan fallback must still run when a user defines a RuleRegistrarMetaInfo bean");
        assertTrue(context.containsBean(BeanNames.RULE_SCAN_META_INFO),
                "Fallback scan should register its own meta-info under the fixed bean name");
    }
}
