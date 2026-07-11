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
import org.rulii.model.UnrulyException;
import org.rulii.spring.config.RuleRegistrar;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies rule bean name collision handling in {@link RuleRegistrar}:
 * <ul>
 *   <li>Two different rule classes resolving to the same rule name fail fast with an
 *       exception naming both classes (previously a generic
 *       {@code BeanDefinitionOverrideException}, or a silent replacement when bean
 *       definition overriding is enabled)</li>
 *   <li>The same rule class discovered twice (overlapping {@code scanBasePackages})
 *       is registered once and skipped quietly on rediscovery</li>
 * </ul>
 *
 * @author Max Arulananthan
 * @since 2.0
 */
class RuleRegistrarCollisionTest {

    private static final String PACKAGE_A = "org.rulii.spring.testrules.collision.a";
    private static final String PACKAGE_B = "org.rulii.spring.testrules.collision.b";

    private RuleRegistrar newRegistrar() {
        return new RuleRegistrar(new StandardEnvironment(), new DefaultResourceLoader());
    }

    @Test
    void sameRuleNameInTwoPackagesFailsNamingBothClasses() {
        DefaultListableBeanFactory registry = new DefaultListableBeanFactory();

        UnrulyException ex = assertThrows(UnrulyException.class, () ->
                newRegistrar().registerRules(new String[] {PACKAGE_A, PACKAGE_B}, registry));

        assertTrue(ex.getMessage().contains(PACKAGE_A + ".DuplicateNameRule"),
                "Exception should name the first colliding class: " + ex.getMessage());
        assertTrue(ex.getMessage().contains(PACKAGE_B + ".DuplicateNameRule"),
                "Exception should name the second colliding class: " + ex.getMessage());
    }

    @Test
    void samePackageScannedTwiceRegistersRuleOnce() {
        DefaultListableBeanFactory registry = new DefaultListableBeanFactory();

        int count = newRegistrar().registerRules(new String[] {PACKAGE_A, PACKAGE_A}, registry);

        assertEquals(1, count, "Rediscovered rule class should be skipped, not re-registered");
        assertTrue(registry.containsBeanDefinition("DuplicateNameRule"));
    }
}
