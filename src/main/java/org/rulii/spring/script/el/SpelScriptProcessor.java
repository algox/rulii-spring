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
package org.rulii.spring.script.el;

import org.rulii.context.RuleContext;
import org.rulii.script.EvaluationException;
import org.rulii.script.Script;
import org.rulii.script.ScriptProcessor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link ScriptProcessor} implementation that evaluates Spring Expression Language (SpEL)
 * scripts within a Rulii {@link RuleContext}.
 *
 * <p>Evaluation is performed against a {@link StandardEvaluationContext} that exposes the
 * rule context's {@link org.rulii.bind.Bindings} as a named variable (e.g. {@code #ctx}),
 * and registers a {@link BindingAccessor} so that binding values can also be resolved
 * as bare property names on the {@code Bindings} object.</p>
 *
 * <p>This processor is created by {@link SpelScriptProcessorFactory} and only handles
 * scripts of type {@link SpelScript}.</p>
 *
 * @author Max Arulananthan
 * @since 1.1
 * @see SpelScriptProcessorFactory
 * @see SpelScriptCompiler
 * @see BindingAccessor
 */
public class SpelScriptProcessor implements ScriptProcessor {

    private static final List<PropertyAccessor> PROPERTY_ACCESSORS = List.of(new BindingAccessor());

    private final String languageName;
    private final String bindingName;

    /**
     * Constructs a new {@code SpelScriptProcessor} with the specified language and binding variable names.
     *
     * @param languageName the language identifier this processor handles (e.g. {@code "el"}); must not be empty
     * @param bindingName  the variable name under which the {@link org.rulii.bind.Bindings} object is
     *                     exposed in the SpEL context (e.g. {@code "ctx"}); must not be empty
     */
    public SpelScriptProcessor(String languageName, String bindingName) {
        super();
        Assert.hasText(languageName, "languageName cannot be empty.");
        Assert.hasText(bindingName, "bindingName cannot be empty.");
        this.languageName = languageName;
        this.bindingName = bindingName;
    }

    /**
     * Returns the language identifier handled by this processor.
     *
     * @return the language name (e.g. {@code "el"})
     */
    @Override
    public String getLanguageName() {
        return languageName;
    }

    /**
     * Returns the variable name under which the {@link org.rulii.bind.Bindings} object is
     * exposed in the SpEL evaluation context.
     *
     * @return the binding variable name (e.g. {@code "ctx"})
     */
    @Override
    public String getBindingName() {
        return bindingName;
    }

    /**
     * Evaluates the given {@link SpelScript} against the supplied {@link RuleContext}.
     *
     * <p>The script must be a {@link SpelScript} instance produced by {@link SpelScriptCompiler}.
     * The rule context's bindings are exposed as a SpEL variable named {@link #getBindingName()},
     * and a {@link BindingAccessor} is registered to allow bare-name property access.</p>
     *
     * @param <T>         the expected return type
     * @param script      the compiled SpEL script to evaluate; must not be {@code null}
     * @param ruleContext the rule context providing bindings for the expression; must not be {@code null}
     * @return the result of evaluating the SpEL expression
     * @throws EvaluationException if evaluation fails at runtime
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T evaluate(Script<T> script, RuleContext ruleContext) {
        Assert.notNull(script, "script cannot be null.");
        Assert.notNull(ruleContext, "ruleContext cannot be null.");

        EvaluationContext scriptContext = buildContext(ruleContext);
        SpelScript<T> spelScript = (SpelScript<T>) script;

        try {
            return (T) spelScript.getExpression().getValue(scriptContext);
        } catch (Exception e) {
            throw new EvaluationException(script.getScript(), e.getMessage(), e);
        }
    }

    /**
     * Builds a {@link StandardEvaluationContext} that exposes the rule context's bindings
     * as a named variable and registers a {@link BindingAccessor} for property-style access.
     *
     * @param ruleContext the rule context whose bindings will be exposed; must not be {@code null}
     * @return a configured SpEL {@link EvaluationContext}
     */
    protected EvaluationContext buildContext(RuleContext ruleContext) {
        Assert.notNull(ruleContext, "ruleContext cannot be null.");

        StandardEvaluationContext result = new StandardEvaluationContext();
        result.setPropertyAccessors(PROPERTY_ACCESSORS);

        Map<String, Object> vars = new HashMap<>();
        vars.put(getBindingName(), ruleContext.getBindings());

        result.setVariables(vars);

        return result;
    }
}
