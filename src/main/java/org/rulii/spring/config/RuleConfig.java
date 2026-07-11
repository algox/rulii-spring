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
package org.rulii.spring.config;

import org.rulii.bind.match.BindingMatchingStrategy;
import org.rulii.bind.match.ParameterResolver;
import org.rulii.context.RuleContextOptions;
import org.rulii.convert.Converter;
import org.rulii.convert.ConverterRegistry;
import org.rulii.registry.RuleRegistry;
import org.rulii.script.ScriptProcessorFactory;
import org.rulii.script.ScriptProcessorManager;
import org.rulii.spring.context.SpringEnabledRuleContextOptions;
import org.rulii.spring.convert.SpringConverterAdapter;
import org.rulii.spring.factory.SpringObjectFactory;
import org.rulii.spring.registry.SpringRuleRegistry;
import org.rulii.spring.text.SpringEnvironmentMessageResolver;
import org.rulii.text.MessageFormatter;
import org.rulii.text.MessageResolver;
import org.rulii.rule.RuleListener;
import org.rulii.ruleset.RuleSetListener;
import org.rulii.trace.RuliiListener;
import org.rulii.trace.Tracer;
import org.rulii.util.reflect.ObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
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
    @Bean(name = BeanNames.BINDING_MATCHING_STRATEGY)
    @ConditionalOnMissingBean(BindingMatchingStrategy.class)
    public BindingMatchingStrategy bindingMatchingStrategy() {
        return BindingMatchingStrategy.builder().build();
    }

    /**
     * Retrieves or creates a ParameterResolver instance if no other bean of type ParameterResolver is available.
     *
     * @return a new instance of ParameterResolver
     */
    @Bean(name = BeanNames.PARAMETER_RESOLVER)
    @ConditionalOnMissingBean(ParameterResolver.class)
    public ParameterResolver parameterResolver() {
        return ParameterResolver.builder().build();
    }

    /**
     * Creates a new MessageResolver instance if no other bean of type MessageResolver is available.
     *
     * @return a new MessageResolver instance
     */
    @Bean(name = BeanNames.MESSAGE_RESOLVER)
    @ConditionalOnMissingBean(MessageResolver.class)
    public MessageResolver messageResolver(Environment environment) {
        return new SpringEnvironmentMessageResolver(environment);
    }

    /**
     * Creates a MessageFormatter instance if no other bean of type MessageFormatter is available.
     *
     * @return a new instance of MessageFormatter
     */
    @Bean(name = BeanNames.MESSAGE_FORMATTER)
    @ConditionalOnMissingBean(MessageFormatter.class)
    public MessageFormatter messageFormatter() {
        return MessageFormatter.builder().build();
    }

    /**
     * Creates an ObjectFactory instance if no other bean of type ObjectFactory is available.
     *
     * @param beanFactory the BeanFactory to use for object creation
     * @return a new ObjectFactory instance
     */
    @Bean(name = BeanNames.OBJECT_FACTORY)
    @ConditionalOnMissingBean(ObjectFactory.class)
    public ObjectFactory objectFactory(BeanFactory beanFactory) {

        if (!(beanFactory instanceof AutowireCapableBeanFactory)) {
            LOGGER.warn("Unable to create SpringObjectFactory. Environment does not support AutowireCapableBeanFactory.");
            return ObjectFactory.builder().build();
        }

        // Use Spring to create the Objects
        return new SpringObjectFactory((AutowireCapableBeanFactory) beanFactory);
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
    public ConverterRegistry converterRegistry(@Autowired(required = false) Set<Converter<?, ?>> converters,
                                               @Autowired(required = false) ConversionService conversionService,
                                               @Value("${rulii.converts.registerDefaults:true}") boolean registerDefaults) {
        ConverterRegistry result = ConverterRegistry.builder(registerDefaults).build();
        // Register custom converters
        if (converters != null && !converters.isEmpty()) {
            converters.forEach(converter -> {
                LOGGER.info("Registering custom Converter [" + converter.getClass() + "]");
                result.register(converter);
            });
        }
        if (conversionService != null) result.register(new SpringConverterAdapter(conversionService));
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
    public RuleRegistry ruleRegistry(@Autowired(required = false) ListableBeanFactory ctx) {
        if (ctx == null) LOGGER.warn("Unable to create SpringRuleRegistry. Environment does not support ListableBeanFactory.");
        return ctx != null ? new SpringRuleRegistry(ctx) : RuleRegistry.builder().build();
    }

    /**
     * Exposes the process-wide {@link ScriptProcessorManager} singleton as a bean and registers
     * any {@link ScriptProcessorFactory} beans found in the application context into it
     * (in addition to factories discovered via {@code META-INF/services}).
     *
     * <p>Note: {@code ScriptProcessorManager} is a JVM-wide singleton — registrations are global,
     * shared across (and outliving) application contexts. Overriding this bean can only replace
     * the registration step, not substitute a different manager instance.
     *
     * @param factories the ScriptProcessorFactory beans to register
     * @return the singleton ScriptProcessorManager instance
     */
    @Bean(BeanNames.SCRIPT_MANAGER)
    @ConditionalOnMissingBean(ScriptProcessorManager.class)
    public ScriptProcessorManager scriptProcessorManager(@Autowired(required = false) List<ScriptProcessorFactory> factories) {
        ScriptProcessorManager result = ScriptProcessorManager.getInstance();

        if (factories != null && !factories.isEmpty()) {
             factories.forEach(factory -> {
                LOGGER.info("Registering custom ScriptProcessor [" + factory.getClass() + "] Language [" + factory.getLanguageName() + "]");
                result.register(factory);
             });
        }

        return result;
    }

    /**
     * Creates a Tracer instance if no other bean of type Tracer is available.
     * Any listener beans found in the application context are automatically registered:
     * {@link RuliiListener} beans receive all event categories; {@link RuleListener} and
     * {@link RuleSetListener} beans receive their respective category only.
     *
     * @param ruliiListeners listener beans to register for all event categories
     * @param ruleListeners listener beans to register for rule events
     * @param ruleSetListeners listener beans to register for ruleset events
     * @return a new Tracer instance with all listener beans registered
     */
    @Bean(BeanNames.TRACER)
    @ConditionalOnMissingBean(Tracer.class)
    public Tracer tracer(@Autowired(required = false) List<RuliiListener> ruliiListeners,
                         @Autowired(required = false) List<RuleListener> ruleListeners,
                         @Autowired(required = false) List<RuleSetListener> ruleSetListeners) {
        Tracer result = Tracer.builder().build();

        if (ruliiListeners != null) {
            ruliiListeners.forEach(listener -> {
                LOGGER.info("Registering RuliiListener [" + listener.getClass() + "]");
                result.addListener(listener);
            });
        }

        if (ruleListeners != null) {
            ruleListeners.stream()
                    .filter(listener -> !(listener instanceof RuliiListener))
                    .forEach(listener -> {
                        LOGGER.info("Registering RuleListener [" + listener.getClass() + "]");
                        result.addListener(listener);
                    });
        }

        if (ruleSetListeners != null) {
            ruleSetListeners.stream()
                    .filter(listener -> !(listener instanceof RuliiListener))
                    .forEach(listener -> {
                        LOGGER.info("Registering RuleSetListener [" + listener.getClass() + "]");
                        result.addListener(listener);
                    });
        }

        return result;
    }

    /**
     * Creates the ExecutorService used for async rule execution if no bean named
     * {@link BeanNames#EXECUTOR_SERVICE} is present. Tied to the application context
     * lifecycle; shut down gracefully when the context closes.
     *
     * @return a new fixed thread pool sized to the available processors (minimum 2)
     */
    @Bean(name = BeanNames.EXECUTOR_SERVICE, destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = BeanNames.EXECUTOR_SERVICE)
    public ExecutorService executorService() {
        int threads = Math.max(2, Runtime.getRuntime().availableProcessors());
        return Executors.newFixedThreadPool(threads);
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
     * @param ruleRegistry the RuleRegistry to use
     * @param tracer the Tracer to use
     * @param executorService the ExecutorService to use for async rule execution
     * @return a new instance of RuleContextOptions
     */
    @Bean(BeanNames.SPRING_CONTEXT_OPTIONS)
    @ConditionalOnMissingBean(RuleContextOptions.class)
    public RuleContextOptions ruleContextOptions(BindingMatchingStrategy matchingStrategy, ParameterResolver parameterResolver,
                                                 MessageFormatter messageFormatter, ConverterRegistry converterRegistry,
                                                 ObjectFactory objectFactory, MessageResolver messageResolver,
                                                 RuleRegistry ruleRegistry, Tracer tracer,
                                                 @Qualifier(BeanNames.EXECUTOR_SERVICE) ExecutorService executorService) {
        return new SpringEnabledRuleContextOptions(matchingStrategy, parameterResolver, messageFormatter,
                converterRegistry, objectFactory, messageResolver, executorService,
                Clock.systemDefaultZone(), Locale.getDefault(), ruleRegistry, tracer);
    }

    /**
     * Creates a RuleBeanDefinitionRegistryPostProcessor instance if {@code @RuleScan} has not been processed
     * (signalled by the internal {@link RuleScanMarker} bean).
     *
     * @param factory the BeanFactory to use
     * @param environment the Environment used to evaluate {@code @Profile} / {@code @Conditional} on rule classes
     * @param resourceLoader the ResourceLoader used for classpath scanning
     * @return a new RuleBeanDefinitionRegistryPostProcessor instance
     */
    @Bean
    @ConditionalOnMissingBean(RuleScanMarker.class)
    public RuleBeanDefinitionRegistryPostProcessor rulePostProcessor(BeanFactory factory, Environment environment, ResourceLoader resourceLoader) {
        List<String> locations = AutoConfigurationPackages.has(factory) ? AutoConfigurationPackages.get(factory) : null;
        LOGGER.warn("@RuleScan not set. rulii will try to auto register the rules starting at location " + locations);
        return new RuleBeanDefinitionRegistryPostProcessor(locations, environment, resourceLoader);
    }
}
