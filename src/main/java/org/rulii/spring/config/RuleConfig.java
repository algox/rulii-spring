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

@AutoConfiguration
public class RuleConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuleConfig.class);

    public RuleConfig() {
        super();
    }

    @Bean
    @ConditionalOnMissingBean(BindingMatchingStrategy.class)
    public BindingMatchingStrategy bindingMatchingStrategy() {
        return BindingMatchingStrategy.builder().build();
    }

    @Bean
    @ConditionalOnMissingBean(ParameterResolver.class)
    public ParameterResolver parameterResolver() {
        return ParameterResolver.builder().build();
    }

    @Bean
    @ConditionalOnMissingBean(MessageFormatter.class)
    public MessageFormatter messageFormatter() {
        return MessageFormatter.builder().build();
    }

    @Bean
    @ConditionalOnMissingBean(MessageResolver.class)
    public MessageResolver messageResolver() {
        return MessageResolver.builder().build();
    }

    @Bean(name = BeanNames.OBJECT_FACTORY_NAME)
    @ConditionalOnMissingBean(ObjectFactory.class)
    public ObjectFactory objectFactory(BeanFactory beanFactory) {
        // Use Spring to create the Objects
        return beanFactory instanceof ListableBeanFactory
                ? new SpringObjectFactory((ListableBeanFactory) beanFactory)
                : ObjectFactory.builder().build();
    }

    @Bean(name = BeanNames.SPRING_CONVERTER_REGISTRY)
    @ConditionalOnMissingBean(ConverterRegistry.class)
    public ConverterRegistry converterRegistry(@Autowired(required = false) ConversionService conversionService,
                                               @Value("${rulii.converts.registerDefaults:true}") boolean registerDefaults) {
        ConverterRegistry result = new DefaultConverterRegistry(registerDefaults);
        result.register(new SpringConverterAdapter(conversionService));
        return result;
    }

    @Bean(BeanNames.RULE_REGISTRY)
    @ConditionalOnMissingBean(RuleRegistry.class)
    public RuleRegistry ruleRegistry(ApplicationContext ctx) {
        return new SpringRuleRegistry(ctx);
    }

    @Bean
    @ConditionalOnMissingBean(RuleContextOptions.class)
    public RuleContextOptions ruleContextOptions(BindingMatchingStrategy matchingStrategy, ParameterResolver parameterResolver,
                                                 MessageFormatter messageFormatter, ConverterRegistry converterRegistry,
                                                 ObjectFactory objectFactory, MessageResolver messageResolver) {
        return new SpringEnabledRuleContextOptions(matchingStrategy, parameterResolver, messageFormatter,
                converterRegistry, objectFactory, messageResolver, Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors())),
                Clock.systemDefaultZone(), Locale.getDefault());
    }

    @Bean
    @ConditionalOnMissingBean(RuleRegistrarMetaInfo.class)
    public RuleBeanDefinitionRegistryPostProcessor rulePostProcessor(BeanFactory factory) {
        LOGGER.warn("@RuleScan not set. Rulii will try to auto register the rules.");
        return new RuleBeanDefinitionRegistryPostProcessor(AutoConfigurationPackages.has(factory) ? AutoConfigurationPackages.get(factory) : null);
    }

    @Bean(BeanNames.RULE_BEAN)
    @ConditionalOnMissingBean(RuleBeans.class)
    public RuleBeans ruleBean(ListableBeanFactory factory) {
        return new RuleBeans(factory);
    }
}
