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
import org.rulii.spring.config.RuleRegistrarMetaInfo;
import org.rulii.spring.test.TestApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that the auto-scan fallback (no {@code @RuleScan}) registers an injectable
 * {@link RuleRegistrarMetaInfo} bean describing the scan, mirroring the {@code @RuleScan}
 * path.
 *
 * <p>Regression test: the fallback previously registered no meta-info at all, so
 * {@code @Autowired RuleRegistrarMetaInfo} worked or failed depending on which path
 * configured the application.
 *
 * @author Max Arulananthan
 * @since 2.0
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestApplication.class)
class FallbackScanMetaInfoTest {

    @Autowired
    private RuleRegistrarMetaInfo metaInfo;

    @Test
    void fallbackScanRegistersMetaInfo() {
        assertNotNull(metaInfo);
        assertTrue(metaInfo.rulePackages().contains("org.rulii.spring.test"),
                "rulePackages should contain the auto-detected base package");
        assertTrue(metaInfo.xmlLocations().isEmpty(), "fallback scan loads no XML locations");
        assertTrue(metaInfo.ruleCount() > 0, "fallback scan should have registered the test rules");
    }
}
