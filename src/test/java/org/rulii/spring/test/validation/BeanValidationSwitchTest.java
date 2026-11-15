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

import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import org.rulii.spring.validation.BeanValidationConfig;
import org.rulii.spring.validation.BeanValidationDependencyCheck;
import org.rulii.spring.validation.HibernateValidatorConfig;
import org.rulii.spring.validation.JpaValidationConfig;
import org.rulii.validation.beans.BeanValidator;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Who validates, for every combination of the switch (the {@code rulii-bean-validation} dependency), its off
 * property ({@code rulii.validation.enabled=false}) and of Hibernate Validator being there or not; and that JPA stays on
 * Hibernate Validator throughout. A missing jar is a filtered class loader: the conditions see the classpath
 * through it.
 *
 * @author Max Arulananthan
 * @since 2.3
 */
class BeanValidationSwitchTest {

    private static final String RULII_BEAN_VALIDATION = "org.rulii.validation.beans";
    private static final String HIBERNATE_VALIDATOR = "org.hibernate.validator";
    private static final String ENABLED = "rulii.validation.enabled=true";
    private static final String DISABLED = "rulii.validation.enabled=false";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BeanValidationConfig.class, HibernateValidatorConfig.class,
                    JpaValidationConfig.class, BeanValidationDependencyCheck.class, ValidationAutoConfiguration.class));

    // ---- both jars on the classpath: the dependency is the switch, the property turns it off ----

    @Test
    void withTheDependencyTheValidatorIsRuliis() {
        runner.run(context -> {
            assertSame(context.getBean(BeanValidator.class), context.getBean(Validator.class).unwrap(BeanValidator.class));
            assertEquals(1, count(context, BeanValidationConfig.class));
            assertEquals(0, count(context, HibernateValidatorConfig.class));
        });
    }

    @Test
    void withThePropertyTrueTheValidatorIsRuliis() {
        runner.withPropertyValues(ENABLED).run(context -> {
            assertSame(context.getBean(BeanValidator.class), context.getBean(Validator.class).unwrap(BeanValidator.class));
            assertEquals(1, count(context, BeanValidationConfig.class));
            assertEquals(0, count(context, HibernateValidatorConfig.class));
            assertEquals(0, count(context, BeanValidationDependencyCheck.class));
        });
    }

    @Test
    void withThePropertyFalseTheValidatorIsHibernates() {
        runner.withPropertyValues(DISABLED).run(context -> {
            assertTrue(validatedByHibernate(context));
            assertEquals(0, count(context, BeanValidator.class));
            assertEquals(1, count(context, HibernateValidatorConfig.class));
            assertEquals(0, count(context, BeanValidationConfig.class));
        });
    }

    // ---- rulii-bean-validation not added: nothing of ours ----

    @Test
    void withoutTheDependencyNothingIsConfigured() {
        runner.withClassLoader(new FilteredClassLoader(RULII_BEAN_VALIDATION)).run(context -> {
            assertTrue(validatedByHibernate(context), "Boot's own validator, over the one provider there is");
            assertEquals(0, count(context, BeanValidationConfig.class));
            assertEquals(0, count(context, HibernateValidatorConfig.class));
            assertEquals(0, count(context, JpaValidationConfig.class));
            assertEquals(0, count(context, HibernatePropertiesCustomizer.class));
            assertEquals(0, count(context, BeanValidationDependencyCheck.class));
        });
    }

    @Test
    void thePropertyWithoutTheDependencyIsReported() {
        runner.withClassLoader(new FilteredClassLoader(RULII_BEAN_VALIDATION)).withPropertyValues(ENABLED).run(context -> {
            assertTrue(validatedByHibernate(context));
            assertEquals(0, count(context, BeanValidationConfig.class));
            assertEquals(1, count(context, BeanValidationDependencyCheck.class));
        });
    }

    // ---- Hibernate Validator removed: rulii's is the only provider ----

    @Test
    void withoutHibernateValidatorRuliiIsWired() {
        runner.withClassLoader(new FilteredClassLoader(HIBERNATE_VALIDATOR)).run(context -> {
            assertSame(context.getBean(BeanValidator.class), context.getBean(Validator.class).unwrap(BeanValidator.class));
            assertEquals(1, count(context, BeanValidationConfig.class));
        });
    }

    @Test
    void withoutHibernateValidatorThePropertyFalseLeavesRuliiToBootUnwired() {
        runner.withClassLoader(new FilteredClassLoader(HIBERNATE_VALIDATOR)).withPropertyValues(DISABLED).run(context -> {
            assertFalse(validatedByHibernate(context), "the one provider there is");
            assertEquals(0, count(context, BeanValidator.class), "not wired into the Spring rulii context");
            assertEquals(0, count(context, BeanValidationConfig.class));
            assertEquals(0, count(context, HibernateValidatorConfig.class));
        });
    }

    // ---- JPA stays on Hibernate Validator while it is there, whatever the switch says ----

    @Test
    void jpaIsHandedHibernateValidatorWhileRuliiValidates() {
        runner.run(context -> {
            ValidatorFactory handed = handedToJpa(context);

            assertTrue(isHibernates(handed), "JPA is not part of the switch");
            assertNotSame(context.getBean(ValidatorFactory.class), handed, "and it is not the application's Validator bean");
            assertSame(handed, handedToJpa(context), "one factory, however often JPA asks");
        });
    }

    @Test
    void jpaIsHandedHibernateValidatorWhenSwitchedOffToo() {
        runner.withPropertyValues(DISABLED).run(context -> assertTrue(isHibernates(handedToJpa(context))));
    }

    @Test
    void withoutHibernateValidatorJpaIsLeftToTheOneProviderThereIs() {
        runner.withClassLoader(new FilteredClassLoader(HIBERNATE_VALIDATOR)).run(context -> {
            assertEquals(0, count(context, JpaValidationConfig.class));
            assertEquals(0, count(context, HibernatePropertiesCustomizer.class));
        });
    }

    @Test
    void aFactoryTheApplicationSetForJpaIsLeftAlone() {
        runner.withPropertyValues(ENABLED).run(context -> {
            Map<String, Object> properties = new HashMap<>();
            properties.put(JpaValidationConfig.VALIDATION_FACTORY_PROPERTY, "the application's own");

            context.getBean(HibernatePropertiesCustomizer.class).customize(properties);

            assertEquals("the application's own", properties.get(JpaValidationConfig.VALIDATION_FACTORY_PROPERTY));
        });
    }

    @Test
    void withoutBootsHibernateModuleJpaIsNotConfigured() {
        runner.withClassLoader(new FilteredClassLoader(HibernatePropertiesCustomizer.class)).run(context ->
                assertEquals(0, count(context, JpaValidationConfig.class)));
    }

    private static ValidatorFactory handedToJpa(ApplicationContext context) {
        Map<String, Object> properties = new HashMap<>();
        context.getBean(HibernatePropertiesCustomizer.class).customize(properties);
        return (ValidatorFactory) properties.get(JpaValidationConfig.VALIDATION_FACTORY_PROPERTY);
    }

    private static boolean isHibernates(ValidatorFactory factory) {
        return factory.getValidator().getClass().getName().startsWith("org.hibernate.validator.");
    }

    private static boolean validatedByHibernate(ApplicationContext context) {
        Validator target = ((LocalValidatorFactoryBean) context.getBean(Validator.class)).getValidator();
        return target.getClass().getName().startsWith("org.hibernate.validator.");
    }

    private static int count(ApplicationContext context, Class<?> type) {
        return context.getBeanNamesForType(type).length;
    }
}
