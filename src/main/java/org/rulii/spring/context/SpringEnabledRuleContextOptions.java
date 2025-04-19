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
package org.rulii.spring.context;

import org.rulii.bind.match.BindingMatchingStrategy;
import org.rulii.bind.match.ParameterResolver;
import org.rulii.context.RuleContextOptions;
import org.rulii.convert.ConverterRegistry;
import org.rulii.text.MessageFormatter;
import org.rulii.text.MessageResolver;
import org.rulii.util.reflect.ObjectFactory;
import org.springframework.util.Assert;

import java.time.Clock;
import java.util.Locale;
import java.util.concurrent.ExecutorService;

/**
 * Represents a set of options for a rule context with Spring integration.
 *
 * @author Max Arulananthan
 * @since 1.0
 *
 */
public class SpringEnabledRuleContextOptions implements RuleContextOptions {

    private final BindingMatchingStrategy matchingStrategy;
    private final ParameterResolver parameterResolver;
    private final MessageFormatter messageFormatter;
    private final ConverterRegistry converterRegistry;
    private final ObjectFactory objectFactory;
    private final MessageResolver messageResolver;
    private final ExecutorService executorService;
    private final Clock clock;
    private final Locale locale;

    /**
     * Constructs a new instance of SpringEnabledRuleContextOptions with the provided parameters.
     *
     * @param matchingStrategy The BindingMatchingStrategy to use
     * @param parameterResolver The ParameterResolver to use
     * @param messageFormatter The MessageFormatter to use
     * @param converterRegistry The ConverterRegistry to use
     * @param objectFactory The ObjectFactory to use
     * @param messageResolver The MessageResolver to use
     * @param executorService The ExecutorService to use
     * @param clock The Clock to use
     * @param locale The Locale to use
     */
    public SpringEnabledRuleContextOptions(BindingMatchingStrategy matchingStrategy, ParameterResolver parameterResolver,
                                           MessageFormatter messageFormatter, ConverterRegistry converterRegistry,
                                           ObjectFactory objectFactory, MessageResolver messageResolver, ExecutorService executorService,
                                           Clock clock, Locale locale) {
        super();
        Assert.notNull(matchingStrategy, "matchingStrategy cannot be null.");
        Assert.notNull(parameterResolver, "parameterResolver cannot be null.");
        Assert.notNull(messageFormatter, "messageFormatter cannot be nul.");
        Assert.notNull(converterRegistry, "converterRegistry cannot be null.");
        Assert.notNull(objectFactory, "objectFactory cannot be null.");
        Assert.notNull(messageResolver, "messageResolver cannot be null.");
        Assert.notNull(executorService, "executorService cannot be null.");
        Assert.notNull(clock, "clock cannot be null.");
        Assert.notNull(locale, "locale cannot be null.");
        this.matchingStrategy = matchingStrategy;
        this.parameterResolver = parameterResolver;
        this.messageFormatter = messageFormatter;
        this.converterRegistry = converterRegistry;
        this.objectFactory = objectFactory;
        this.messageResolver = messageResolver;
        this.executorService = executorService;
        this.clock = clock;
        this.locale = locale;
    }

    @Override
    public BindingMatchingStrategy getMatchingStrategy() {
        return matchingStrategy;
    }

    @Override
    public ParameterResolver getParameterResolver() {
        return parameterResolver;
    }

    @Override
    public MessageResolver getMessageResolver() {
        return messageResolver;
    }

    @Override
    public MessageFormatter getMessageFormatter() {
        return messageFormatter;
    }

    @Override
    public ObjectFactory getObjectFactory() {
        return objectFactory;
    }

    @Override
    public ConverterRegistry getConverterRegistry() {
        return converterRegistry;
    }

    @Override
    public Clock getClock() {
        return clock;
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    @Override
    public ExecutorService getExecutorService() {
        return executorService;
    }

    @Override
    public String toString() {
        return "SpringEnabledRuleContextOptions{" +
                "matchingStrategy=" + matchingStrategy +
                ", parameterResolver=" + parameterResolver +
                ", messageFormatter=" + messageFormatter +
                ", converterRegistry=" + converterRegistry +
                ", objectFactory=" + objectFactory +
                ", clock=" + clock +
                ", locale=" + locale +
                ", messageResolver=" + messageResolver +
                ", executorService=" + executorService +
                '}';
    }
}
