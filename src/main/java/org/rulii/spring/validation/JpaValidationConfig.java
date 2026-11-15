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
package org.rulii.spring.validation;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.spi.ValidationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.util.ClassUtils;

import java.util.List;

/**
 * Keeps JPA validating entities with Hibernate Validator, as it did before {@code rulii-bean-validation} was added.
 *
 * <p>Spring Boot does not hand its validator to JPA: Hibernate bootstraps Bean Validation on its own and takes the
 * first provider on the classpath. With {@code rulii-bean-validation} added beside Hibernate Validator that would be
 * whichever dependency happens to come first. This configuration takes the chance out of it: it hands JPA the
 * factory Hibernate would have built for itself with Hibernate Validator alone on the classpath, whatever
 * {@code rulii.validation.enabled} says. rulii bean validation is for the application's {@code Validator} bean (web
 * requests, {@code @Validated} services, injected validators); entity validation at persist time is left as it was.
 * rulii's own annotations ({@code @Validate}, {@code @Expr}) on an entity are therefore not checked by JPA; validate
 * the entity through the {@code Validator} bean before saving it for that.</p>
 *
 * <p>Applies only while both providers are on the classpath. An application that removes Hibernate Validator has
 * one provider left, rulii's, and JPA bootstraps it by itself. A factory the application has already set for JPA is
 * left alone.</p>
 *
 * @author Max Arulananthan
 * @since 2.3
 */
@AutoConfiguration
@ConditionalOnClass(name = {"org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer",
        HibernateValidatorConfig.RULII_PROVIDER, HibernateValidatorConfig.HIBERNATE_VALIDATOR})
public class JpaValidationConfig implements DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(JpaValidationConfig.class);

    /**
     * The JPA property a {@code ValidatorFactory} is handed over under.
     */
    public static final String VALIDATION_FACTORY_PROPERTY = "jakarta.persistence.validation.factory";

    private ValidatorFactory hibernateValidatorFactory;

    public JpaValidationConfig() {
        super();
    }

    /**
     * Hands JPA a Hibernate Validator factory, unless the application has set one itself.
     *
     * @return the customizer.
     */
    @Bean
    public HibernatePropertiesCustomizer hibernateValidatorForJpa() {
        return properties -> properties.computeIfAbsent(VALIDATION_FACTORY_PROPERTY, key -> hibernateValidatorFactory());
    }

    /**
     * The factory the default bootstrap gives when Hibernate Validator is the only provider there is: what Hibernate
     * builds for itself in an application without rulii bean validation. Built once, on first use.
     */
    private synchronized ValidatorFactory hibernateValidatorFactory() {
        if (hibernateValidatorFactory == null) {
            ValidationProvider<?> hibernateValidator = (ValidationProvider<?>) BeanUtils.instantiateClass(
                    ClassUtils.resolveClassName(HibernateValidatorConfig.HIBERNATE_VALIDATOR, JpaValidationConfig.class.getClassLoader()));
            hibernateValidatorFactory = Validation.byDefaultProvider()
                    .providerResolver(() -> List.of(hibernateValidator))
                    .configure()
                    .buildValidatorFactory();
            LOGGER.info("JPA validates entities with Hibernate Validator; rulii bean validation applies to the "
                    + "application's Validator bean.");
        }

        return hibernateValidatorFactory;
    }

    @Override
    public synchronized void destroy() {
        if (hibernateValidatorFactory != null) {
            hibernateValidatorFactory.close();
            hibernateValidatorFactory = null;
        }
    }
}
