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
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.Assert;

/**
 * A {@link ScriptProcessor} implementation that evaluates Spring Expression Language (SpEL)
 * scripts within a Rulii {@link RuleContext}.
 *
 * <p>Evaluation is performed against a {@link StandardEvaluationContext} whose root object
 * is the rule context's {@link org.rulii.bind.Bindings}, with the same bindings also exposed
 * as a named variable (e.g. {@code #ctx}). A {@link BindingAccessor} resolves binding names
 * on the bindings object, and Spring's default reflective accessor remains registered so
 * expressions can navigate into binding values. All of the following forms are equivalent:
 * {@code age >= 18}, {@code #ctx.age >= 18}, {@code person.name}, {@code #ctx.person.name}.</p>
 *
 * <p><strong>Trust model:</strong> expressions run with the full power of
 * {@link StandardEvaluationContext} — including {@code T()} type references, constructors,
 * and static methods. Rule expressions are trusted code, exactly like Spring bean XML;
 * never assemble them from untrusted user input.</p>
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

    private static final BindingAccessor BINDING_ACCESSOR = new BindingAccessor();

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
    public String getBindingsName() {
        return bindingName;
    }

    /**
     * Evaluates the given {@link SpelScript} against the supplied {@link RuleContext}.
     *
     * <p>The script must be a {@link SpelScript} instance produced by {@link SpelScriptCompiler}.
     * The rule context's bindings are exposed as a SpEL variable named {@link #getBindingsName()},
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

        if (!(script instanceof SpelScript)) {
            throw new EvaluationException(script.getScript(), "SpelScriptProcessor (language \"" + languageName
                    + "\") can only evaluate SpelScript instances; received [" + script.getClass().getName() + "].");
        }

        SpelScript<T> spelScript = (SpelScript<T>) script;
        EvaluationContext scriptContext = buildContext(ruleContext);
        Class<?> returnType = spelScript.getReturnType();

        try {
            // Apply SpEL type conversion only for concrete declared types: actions declare
            // void (nothing to convert to) and Object means "whatever the expression yields".
            if (returnType == null || returnType == Object.class || returnType == void.class || returnType == Void.class) {
                return (T) spelScript.getExpression().getValue(scriptContext);
            }
            return (T) spelScript.getExpression().getValue(scriptContext, returnType);
        } catch (Exception e) {
            throw new EvaluationException(script.getScript(), e.getMessage(), e);
        }
    }

    /**
     * Builds a {@link StandardEvaluationContext} rooted at the rule context's bindings.
     * The bindings are also exposed as a named variable ({@link #getBindingsName()}), and a
     * {@link BindingAccessor} is added ahead of the default reflective accessor so binding
     * names resolve directly while binding values remain navigable as regular objects.
     *
     * @param ruleContext the rule context whose bindings will be exposed; must not be {@code null}
     * @return a configured SpEL {@link EvaluationContext}
     */
    protected EvaluationContext buildContext(RuleContext ruleContext) {
        Assert.notNull(ruleContext, "ruleContext cannot be null.");

        StandardEvaluationContext result = new StandardEvaluationContext(ruleContext.getBindings());
        result.addPropertyAccessor(BINDING_ACCESSOR);
        result.setVariable(getBindingsName(), ruleContext.getBindings());

        return result;
    }
}
