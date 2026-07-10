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

/**
 * Utility class that contains constant names for beans used in the framework.
 *
 * @author Max Arulananthan
 * @since 1.0
 *
 */
public final class BeanNames {

    /** Bean name for the {@link org.rulii.bind.match.BindingMatchingStrategy} bean. */
    public static final String BINDING_MATCHING_STRATEGY    = "rulii.bindingMatchingStrategy";

    /** Bean name for the {@link org.rulii.text.MessageResolver} bean. */
    public static final String MESSAGE_RESOLVER             = "rulii.messageResolver";

    /** Bean name for the {@link org.rulii.bind.match.ParameterResolver} bean. */
    public static final String PARAMETER_RESOLVER           = "rulii.parameterResolver";

    /** Bean name for the {@link org.rulii.text.MessageFormatter} bean. */
    public static final String MESSAGE_FORMATTER            = "rulii.messageFormatter";

    /** Bean name for the {@link org.rulii.util.reflect.ObjectFactory} bean. */
    public static final String OBJECT_FACTORY               = "rulii.objectFactory";

    /** Bean name for the {@link org.rulii.convert.ConverterRegistry} bean. */
    public static final String SPRING_CONVERTER_REGISTRY    = "rulii.converterRegistry";

    /** Bean name for the {@link org.rulii.registry.RuleRegistry} bean. */
    public static final String RULE_REGISTRY                = "rulii.ruleRegistry";

    /** Bean name for the {@link org.rulii.context.RuleContextOptions} bean. */
    public static final String SPRING_CONTEXT_OPTIONS       = "rulii.springOptions";

    /** Bean name for the {@link org.rulii.script.ScriptProcessorManager} bean. */
    public static final String SCRIPT_MANAGER               = "rulii.scriptManager";

    /** Bean name for the {@link org.rulii.trace.Tracer} bean. */
    public static final String TRACER                       = "rulii.tracer";

    /** Bean name for the {@link java.util.concurrent.ExecutorService} bean. */
    public static final String EXECUTOR_SERVICE             = "rulii.executorService";

    private BeanNames() {
        super();
    }

}
