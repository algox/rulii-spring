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

import jakarta.validation.Configuration;
import jakarta.validation.Validator;
import jakarta.validation.valueextraction.ValueExtractor;
import org.rulii.context.RuleContextOptions;
import org.rulii.registry.RuleRegistry;
import org.rulii.validation.beans.BeanValidator;
import org.rulii.validation.beans.bootstrap.RuliiConfiguration;
import org.rulii.validation.beans.bootstrap.RuliiValidationProvider;
import org.rulii.validation.beans.mapper.ConstraintMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.validation.autoconfigure.ValidationConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * Makes rulii bean validation the application's Bean Validation provider, wired into the Spring rulii context.
 *
 * <p>Runs before Spring Boot's validation auto-configuration and defines the {@code defaultValidator} bean Boot
 * would otherwise define: a {@link LocalValidatorFactoryBean} pinned to {@link RuliiValidationProvider}, so the
 * provider is selected whatever else is on the classpath (the Jakarta bootstrap otherwise takes the first provider
 * it finds, in classpath order; Boot's validation starter brings Hibernate Validator). The provider's
 * {@link RuliiConfiguration} is given the Spring {@link RuleRegistry} (so a {@code @Validate} name is a bean name),
 * the Spring {@link RuleContextOptions}, every {@link ConstraintMapper} and {@link ValueExtractor} bean, and the
 * {@code rulii.validation.failFast} / {@code rulii.validation.registerFlows} properties; Boot's
 * {@link ValidationConfigurationCustomizer} beans are applied after that. Boot's method validation post-processor
 * validates through this bean, so {@code @Validated} services work unchanged.</p>
 *
 * <p>The unwrapped {@link BeanValidator} is a bean too, for code that wants {@code validateIn} or the materialized
 * flows without unwrapping. {@code rulii.validation.enabled=false} switches all of this off and leaves the
 * {@code Validator} bean to Boot.</p>
 *
 * @author Max Arulananthan
 * @since 2.2
 */
@AutoConfiguration(beforeName = "org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration")
@ConditionalOnClass(name = {"org.rulii.validation.beans.BeanValidator",
        "org.springframework.boot.validation.autoconfigure.ValidationConfigurationCustomizer"})
@ConditionalOnProperty(name = "rulii.validation.enabled", havingValue = "true", matchIfMissing = true)
public class BeanValidationConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(BeanValidationConfig.class);

    /**
     * Name of the validator bean, the one Boot's validation auto-configuration uses, so that Boot's primary-validator
     * handling and method validation find it as they would find Boot's own.
     */
    public static final String VALIDATOR_BEAN_NAME = "defaultValidator";

    public BeanValidationConfig() {
        super();
    }

    /**
     * The application's {@code Validator}: a {@link LocalValidatorFactoryBean} over the rulii provider, configured
     * from the Spring rulii context. Static, like Boot's, so this configuration class is not instantiated early.
     *
     * @param ruleRegistry the Spring rule registry.
     * @param options the Spring rule context options.
     * @param mappers constraint mapper beans, if any.
     * @param extractors value extractor beans, if any.
     * @param customizers Boot's configuration customizers, if any.
     * @param failFast {@code rulii.validation.failFast}; stop at the first violation.
     * @param registerFlows {@code rulii.validation.registerFlows}; register each class's validation flow in the registry.
     * @return the validator factory bean.
     */
    @Bean(name = VALIDATOR_BEAN_NAME)
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean(Validator.class)
    public static LocalValidatorFactoryBean defaultValidator(ObjectProvider<RuleRegistry> ruleRegistry,
                                                             ObjectProvider<RuleContextOptions> options,
                                                             ObjectProvider<ConstraintMapper<?>> mappers,
                                                             ObjectProvider<ValueExtractor<?>> extractors,
                                                             ObjectProvider<ValidationConfigurationCustomizer> customizers,
                                                             @Value("${rulii.validation.failFast:false}") boolean failFast,
                                                             @Value("${rulii.validation.registerFlows:false}") boolean registerFlows) {
        LocalValidatorFactoryBean result = new LocalValidatorFactoryBean();
        result.setProviderClass(RuliiValidationProvider.class);
        result.setConfigurationInitializer(configuration -> {
            configure(configuration, ruleRegistry, options, mappers, extractors, failFast, registerFlows);
            customizers.orderedStream().forEach(customizer -> customizer.customize(configuration));
        });
        return result;
    }

    /**
     * The rulii validator behind the {@code Validator} bean, for code that wants {@code validateIn} or the
     * materialized rules and flows.
     *
     * @param validator the {@code Validator} bean.
     * @return the unwrapped validator.
     */
    @Bean
    @ConditionalOnBean(Validator.class)
    @ConditionalOnMissingBean(BeanValidator.class)
    public BeanValidator beanValidator(Validator validator) {
        return validator.unwrap(BeanValidator.class);
    }

    private static void configure(Configuration<?> configuration, ObjectProvider<RuleRegistry> ruleRegistry,
                                  ObjectProvider<RuleContextOptions> options, ObjectProvider<ConstraintMapper<?>> mappers,
                                  ObjectProvider<ValueExtractor<?>> extractors, boolean failFast, boolean registerFlows) {
        if (!(configuration instanceof RuliiConfiguration rulii)) {
            LOGGER.warn("Validator configuration is [{}], not rulii's; the Spring rulii context is not applied.",
                    configuration.getClass().getName());
            return;
        }

        RuleRegistry registry = ruleRegistry.getIfAvailable();
        RuleContextOptions contextOptions = options.getIfAvailable();

        if (registry != null) rulii.registry(registry);
        if (contextOptions != null) rulii.options(contextOptions);
        rulii.failFast(failFast);
        rulii.registerFlows(registerFlows);

        for (ConstraintMapper<?> mapper : mappers.orderedStream().toList()) {
            rulii.mapper(mapper);
        }

        for (ValueExtractor<?> extractor : extractors.orderedStream().toList()) {
            rulii.addValueExtractor(extractor);
        }
    }
}
