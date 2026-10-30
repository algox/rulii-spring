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

import jakarta.validation.Validator;
import jakarta.validation.spi.ValidationProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.validation.MessageInterpolatorFactory;
import org.springframework.boot.validation.autoconfigure.ValidationConfigurationCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.util.ClassUtils;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * Keeps Hibernate Validator the application's Bean Validation provider when rulii bean validation is opted out.
 *
 * <p>rulii-bean-validation comes with rulii-spring, so an application that still has Hibernate Validator has two
 * providers on its classpath, and the Jakarta bootstrap would take whichever comes first. With
 * {@code rulii.validation.enabled=false} this configuration runs in place of {@link BeanValidationConfig} and, when
 * Hibernate Validator is present, defines the {@code defaultValidator} bean Boot would define, pinned to it: the same
 * {@link LocalValidatorFactoryBean}, with Boot's message interpolator and the {@link ValidationConfigurationCustomizer}
 * beans applied, so opting out gives the application what it had before. Without Hibernate Validator the bean is
 * left to Boot, which bootstraps the one provider there is, rulii's, without the Spring rulii wiring.</p>
 *
 * @author Max Arulananthan
 * @since 2.2
 */
@AutoConfiguration(beforeName = "org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration")
@ConditionalOnClass(name = {BeanValidationOptOutConfig.HIBERNATE_VALIDATOR,
        "org.springframework.boot.validation.autoconfigure.ValidationConfigurationCustomizer"})
@ConditionalOnProperty(name = "rulii.validation.enabled", havingValue = "false")
public class BeanValidationOptOutConfig {

    /**
     * Hibernate Validator's provider class, by name: it is not a dependency of this module.
     */
    static final String HIBERNATE_VALIDATOR = "org.hibernate.validator.HibernateValidator";

    public BeanValidationOptOutConfig() {
        super();
    }

    /**
     * The application's {@code Validator}: Boot's {@link LocalValidatorFactoryBean}, pinned to Hibernate Validator.
     * Static, like Boot's, so this configuration class is not instantiated early.
     *
     * @param applicationContext the application context, the message source Boot interpolates messages through.
     * @param customizers Boot's configuration customizers, if any.
     * @return the validator factory bean.
     */
    @Bean(name = BeanValidationConfig.VALIDATOR_BEAN_NAME)
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean(Validator.class)
    public static LocalValidatorFactoryBean defaultValidator(ApplicationContext applicationContext,
                                                             ObjectProvider<ValidationConfigurationCustomizer> customizers) {
        LocalValidatorFactoryBean result = new LocalValidatorFactoryBean();
        result.setProviderClass(hibernateValidator());
        result.setMessageInterpolator(new MessageInterpolatorFactory(applicationContext).getObject());
        result.setConfigurationInitializer(configuration ->
                customizers.orderedStream().forEach(customizer -> customizer.customize(configuration)));
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends ValidationProvider<?>> hibernateValidator() {
        return (Class<? extends ValidationProvider<?>>) ClassUtils.resolveClassName(HIBERNATE_VALIDATOR,
                BeanValidationOptOutConfig.class.getClassLoader());
    }
}
