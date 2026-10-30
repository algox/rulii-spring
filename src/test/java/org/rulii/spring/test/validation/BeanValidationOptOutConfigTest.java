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

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.rulii.spring.validation.BeanValidationConfig;
import org.rulii.spring.validation.BeanValidationOptOutConfig;
import org.rulii.validation.beans.BeanValidator;
import org.rulii.validation.beans.bootstrap.RuliiConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code rulii.validation.enabled=false} keeps Hibernate Validator: no rulii validator bean, and the
 * {@code Validator} bean is pinned to Hibernate Validator although rulii's provider comes first on the classpath.
 *
 * @author Max Arulananthan
 * @since 2.2
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = BeanValidationOptOutConfigTest.Config.class, properties = "rulii.validation.enabled=false")
class BeanValidationOptOutConfigTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class Config {
    }

    @Autowired
    private ApplicationContext context;

    @Autowired
    private Validator validator;

    @Test
    void nothingOfOursIsConfigured() {
        assertEquals(0, context.getBeanNamesForType(BeanValidator.class).length);
        assertFalse(context.containsBean(BeanValidationConfig.class.getName()));
        assertTrue(context.containsBean(BeanValidationOptOutConfig.class.getName()));
    }

    @Test
    void theValidatorIsHibernatesWhateverTheClasspathOrder() {
        // rulii's provider is the one the Jakarta bootstrap would pick on this classpath.
        assertInstanceOf(RuliiConfiguration.class, Validation.byDefaultProvider().configure());
        // The application's Validator is Hibernate Validator's regardless.
        Validator target = ((LocalValidatorFactoryBean) validator).getValidator();
        assertTrue(target.getClass().getName().startsWith("org.hibernate.validator."), target.getClass().getName());
        assertThrows(ValidationException.class, () -> validator.unwrap(BeanValidator.class));
    }

    @Test
    void theValidatorValidates() {
        Set<ConstraintViolation<Order>> violations = validator.validate(new Order());

        assertEquals(1, violations.size());
        assertEquals("code", violations.iterator().next().getPropertyPath().toString());
    }

    public static class Order {
        @NotNull
        public String code;
    }
}
