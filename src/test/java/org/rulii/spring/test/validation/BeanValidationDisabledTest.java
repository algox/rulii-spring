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
package org.rulii.spring.test.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.rulii.spring.validation.BeanValidationConfig;
import org.rulii.validation.beans.BeanValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * {@code rulii.validation.enabled=false} leaves Bean Validation alone: no rulii validator bean, and the
 * {@code Validator} bean is whatever Boot configures.
 *
 * @author Max Arulananthan
 * @since 2.2
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = BeanValidationDisabledTest.Config.class, properties = "rulii.validation.enabled=false")
class BeanValidationDisabledTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class Config {
    }

    @Autowired
    private ApplicationContext context;

    @Test
    void nothingOfOursIsConfigured() {
        assertEquals(0, context.getBeanNamesForType(BeanValidator.class).length);
        assertFalse(context.containsBean(BeanValidationConfig.class.getName()));
    }
}
