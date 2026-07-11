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
import org.rulii.model.UnrulyException;
import org.rulii.spring.annotation.RuleScan;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that {@link RuleScan#xmlLocations()} fails fast when an explicit XML file
 * location (ending with {@code .xml}) does not exist, instead of silently registering
 * no rules.
 *
 * @author Max Arulananthan
 * @since 2.0
 */
class RuleScanXmlMissingFileTest {

    @Configuration
    @RuleScan(xmlLocations = "classpath:rules/xml-scan/no-such-file.xml")
    static class MissingFileConfig {}

    @Test
    void missingExplicitXmlFileFailsStartup() {
        Exception ex = assertThrows(Exception.class, () -> {
            try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(MissingFileConfig.class)) {
                context.getBeanDefinitionCount();
            }
        });
        assertTrue(hasCause(ex, UnrulyException.class),
                "Startup failure should be caused by UnrulyException, but was: " + ex);
    }

    private static boolean hasCause(Throwable t, Class<? extends Throwable> type) {
        while (t != null) {
            if (type.isInstance(t)) return true;
            t = t.getCause();
        }
        return false;
    }
}
