/*
 * This software is licensed under the Apache 2 license, quoted below.
 *
 * Copyright (c) 1999-2025, Algorithmx Inc.
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
package org.rulii.spring.config;

import org.rulii.bind.match.BindingMatchingStrategy;
import org.rulii.bind.match.ParameterResolver;
import org.rulii.context.RuleContextOptions;
import org.rulii.convert.ConverterRegistry;
import org.rulii.convert.DefaultConverterRegistry;
import org.rulii.registry.RuleRegistry;
import org.rulii.spring.RuleBeans;
import org.rulii.spring.context.SpringEnabledRuleContextOptions;
import org.rulii.spring.convert.SpringConverterAdapter;
import org.rulii.spring.factory.SpringObjectFactory;
import org.rulii.spring.registry.SpringRuleRegistry;
import org.rulii.text.MessageFormatter;
import org.rulii.text.MessageResolver;
import org.rulii.util.reflect.ObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.ConversionService;

import java.time.Clock;
import java.util.Locale;
import java.util.concurrent.Executors;

/**
 * Configuration class for setting up rules in the system.
 *
 * @author Max Arulananthan
 * @since 1.0
 *
 */
@AutoConfiguration
public class RuleConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuleConfig.class);

    public RuleConfig() {
        super();
    }

    /**
     * Creates a BindingMatchingStrategy instance if no other bean of type BindingMatchingStrategy is available.
     *
     * @return a new instance of BindingMatchingStrategy
     */
    @Bean
    @ConditionalOnMissingBean(BindingMatchingStrategy.class)
    public BindingMatchingStrategy bindingMatchingStrategy() {
        return BindingMatchingStrategy.builder().build();
    }

    /**
     * Retrieves or creates a ParameterResolver instance if no other bean of type ParameterResolver is available.
     *
     * @return a new instance of ParameterResolver
     */
    @Bean
    @ConditionalOnMissingBean(ParameterResolver.class)
    public ParameterResolver parameterResolver() {
        return ParameterResolver.builder().build();
    }

    /**
     * Creates a MessageFormatter instance if no other bean of type MessageFormatter is available.
     *
     * @return a new instance of MessageFormatter
     */
    @Bean
    @ConditionalOnMissingBean(MessageFormatter.class)
    public MessageFormatter messageFormatter() {
        return MessageFormatter.builder().build();
    }

    /**
     * Creates a new MessageResolver instance if no other bean of type MessageResolver is available.
     *
     * @return a new MessageResolver instance
     */
    @Bean
    @ConditionalOnMissingBean(MessageResolver.class)
    public MessageResolver messageResolver() {
        return MessageResolver.builder().build();
    }

    /**
     * Creates an ObjectFactory instance if no other bean of type ObjectFactory is available.
     *
     * @param beanFactory the BeanFactory to use for object creation
     * @return a new ObjectFactory instance
     */
    @Bean(name = BeanNames.OBJECT_FACTORY_NAME)
    @ConditionalOnMissingBean(ObjectFactory.class)
    public ObjectFactory objectFactory(BeanFactory beanFactory) {
        // Use Spring to create the Objects
        return beanFactory instanceof ListableBeanFactory
                ? new SpringObjectFactory((ListableBeanFactory) beanFactory)
                : ObjectFactory.builder().build();
    }

    /**
     * Retrieves or creates a ConverterRegistry instance if no other bean of type ConverterRegistry is available.
     *
     * @param conversionService    the ConversionService to use for conversion
     * @param registerDefaults     a boolean indicating whether to register default converters
     * @return a new instance of ConverterRegistry
     */
    @Bean(name = BeanNames.SPRING_CONVERTER_REGISTRY)
    @ConditionalOnMissingBean(ConverterRegistry.class)
    public ConverterRegistry converterRegistry(@Autowired(required = false) ConversionService conversionService,
                                               @Value("${rulii.converts.registerDefaults:true}") boolean registerDefaults) {
        ConverterRegistry result = new DefaultConverterRegistry(registerDefaults);
        result.register(new SpringConverterAdapter(conversionService));
        return result;
    }

    /**
     * Creates a new RuleRegistry instance if no other bean of type RuleRegistry is available.
     *
     * @param ctx the ApplicationContext to use for rule management
     * @return a new instance of RuleRegistry
     */
    @Bean(BeanNames.RULE_REGISTRY)
    @ConditionalOnMissingBean(RuleRegistry.class)
    public RuleRegistry ruleRegistry(ApplicationContext ctx) {
        return new SpringRuleRegistry(ctx);
    }

    /**
     * Creates a RuleContextOptions instance if no other bean of type RuleContextOptions is available.
     *
     * @param matchingStrategy the BindingMatchingStrategy to use
     * @param parameterResolver the ParameterResolver to use
     * @param messageFormatter the MessageFormatter to use
     * @param converterRegistry the ConverterRegistry to use
     * @param objectFactory the ObjectFactory to use
     * @param messageResolver the MessageResolver to use
     * @return a new instance of RuleContextOptions
     */
    @Bean
    @ConditionalOnMissingBean(RuleContextOptions.class)
    public RuleContextOptions ruleContextOptions(BindingMatchingStrategy matchingStrategy, ParameterResolver parameterResolver,
                                                 MessageFormatter messageFormatter, ConverterRegistry converterRegistry,
                                                 ObjectFactory objectFactory, MessageResolver messageResolver) {
        return new SpringEnabledRuleContextOptions(matchingStrategy, parameterResolver, messageFormatter,
                converterRegistry, objectFactory, messageResolver, Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors())),
                Clock.systemDefaultZone(), Locale.getDefault());
    }

    /**
     * Creates a RuleBeanDefinitionRegistryPostProcessor instance if no other bean of type RuleRegistrarMetaInfo is available.
     *
     * @param factory the BeanFactory to use
     * @return a new RuleBeanDefinitionRegistryPostProcessor instance
     */
    @Bean
    @ConditionalOnMissingBean(RuleRegistrarMetaInfo.class)
    public RuleBeanDefinitionRegistryPostProcessor rulePostProcessor(BeanFactory factory) {
        LOGGER.warn("@RuleScan not set. Rulii will try to auto register the rules.");
        return new RuleBeanDefinitionRegistryPostProcessor(AutoConfigurationPackages.has(factory) ? AutoConfigurationPackages.get(factory) : null);
    }

    /**
     * Creates a RuleBeans instance if no other bean of type RuleBeans is available.
     *
     * @param factory the ListableBeanFactory to use for initializing RuleBeans
     * @return a new RuleBeans instance
     */
    @Bean(BeanNames.RULE_BEAN)
    @ConditionalOnMissingBean(RuleBeans.class)
    public RuleBeans ruleBean(ListableBeanFactory factory) {
        return new RuleBeans(factory);
    }
}
