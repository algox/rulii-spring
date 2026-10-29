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
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.HibernateValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.rulii.context.RuleContextOptions;
import org.rulii.registry.RuleRegistry;
import org.rulii.rule.Rule;
import org.rulii.validation.RuleViolation;
import org.rulii.validation.beans.BeanValidator;
import org.rulii.validation.beans.Validate;
import org.rulii.validation.rules.Validators;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The auto-configured {@code Validator} is rulii's, wired into the Spring rulii context, with Hibernate Validator
 * on the test classpath and ignored.
 *
 * @author Max Arulananthan
 * @since 2.2
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = BeanValidationConfigTest.Config.class, properties = "rulii.validation.failFast=false")
class BeanValidationConfigTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class Config {

        @Bean
        public Rule customerRequired() {
            return Validators.notNull(Validators.binding("customer")).name("customerRequired").build();
        }

        @Bean
        public OrderService orderService() {
            return new OrderService();
        }
    }

    @Autowired
    private Validator validator;

    @Autowired
    private BeanValidator beanValidator;

    @Autowired
    private RuleRegistry ruleRegistry;

    @Autowired
    private RuleContextOptions options;

    @Autowired
    private OrderService orderService;

    @Test
    void theValidatorIsRuliisWhateverTheClasspathOrder() {
        // Hibernate Validator is on the classpath, a candidate the Jakarta bootstrap could pick first.
        assertNotNull(HibernateValidator.class.getName());
        // The application's Validator is ours regardless.
        assertSame(beanValidator, validator.unwrap(BeanValidator.class));
    }

    @Test
    void theValidatorIsWiredIntoTheSpringRuliiContext() {
        assertSame(ruleRegistry, beanValidator.getRuleRegistry(), "@Validate names resolve to Spring beans");
        assertSame(options.getObjectFactory(), beanValidator.getOptions().getObjectFactory(), "the Spring options are the validator's");
    }

    @Test
    void validateRulesAreSpringBeans() {
        Order order = new Order();
        order.code = "x";

        Set<ConstraintViolation<Order>> violations = validator.validate(order);

        assertEquals(List.of(":customerRequired", "code:code_size"), report(violations));
    }

    @Test
    void methodValidationRunsThroughTheSameValidator() {
        ConstraintViolationException thrown = assertThrows(ConstraintViolationException.class,
                () -> orderService.place(null));
        assertEquals(1, thrown.getConstraintViolations().size());
        assertTrue(thrown.getConstraintViolations().iterator().next().getPropertyPath().toString().startsWith("place."));
    }

    private static List<String> report(Set<? extends ConstraintViolation<?>> violations) {
        List<String> result = new ArrayList<>();

        for (ConstraintViolation<?> violation : violations) {
            result.add(violation.getPropertyPath() + ":" + ((RuleViolation) violation).getRuleName());
        }

        result.sort(null);
        return result;
    }

    @Validate("customerRequired")
    public static class Order {
        @NotNull @Size(min = 2, max = 5)
        public String code;

        public String customer;
    }

    @Validated
    public static class OrderService {

        public void place(@NotNull Order order) {
            // nothing to do: the parameter constraint is the point
        }
    }
}
