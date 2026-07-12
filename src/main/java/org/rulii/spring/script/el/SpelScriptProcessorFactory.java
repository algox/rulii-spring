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

// Deliberately uses rulii's repackaged Assert (not org.springframework.util.Assert): this class
// must be loadable WITHOUT Spring on the classpath so that isAvailable() can gracefully report
// false during ServiceLoader discovery when spring-expression (a provided dependency) is absent.
import org.rulii.lib.spring.util.Assert;
import org.rulii.script.ScriptCompiler;
import org.rulii.script.ScriptOptions;
import org.rulii.script.ScriptProcessor;
import org.rulii.script.ScriptProcessorFactory;

/**
 * A {@link ScriptProcessorFactory} for the Spring Expression Language (SpEL) scripting engine.
 *
 * <p>This factory is the entry point for the SpEL script-processing pipeline. It is registered
 * via the Java {@link java.util.ServiceLoader} mechanism (see
 * {@code META-INF/services/org.rulii.script.ScriptProcessorFactory}) and is automatically
 * discovered by the Rulii framework.</p>
 *
 * <p>The factory produces:
 * <ul>
 *   <li>A {@link SpelScriptCompiler} that parses SpEL expression strings into
 *       {@link SpelScript} instances.</li>
 *   <li>A {@link SpelScriptProcessor} that evaluates those compiled scripts against
 *       a Rulii {@link org.rulii.context.RuleContext}.</li>
 * </ul>
 * </p>
 *
 * <p>The default language identifier is {@value #LANGUAGE_NAME} and the default
 * binding variable name is taken from {@link ScriptOptions#DEFAULT}.</p>
 *
 * @author Max Arulananthan
 * @since 1.1
 * @see SpelScriptCompiler
 * @see SpelScriptProcessor
 */
public class SpelScriptProcessorFactory implements ScriptProcessorFactory {

    /** The language identifier used to select this factory: {@value}. */
    public static final String LANGUAGE_NAME = "el";

    /** Probed once at class load: false when spring-expression is not on the classpath. */
    private static final boolean SPEL_PRESENT = detectSpel();

    private final String languageName;
    private final String bindingName;

    private static boolean detectSpel() {
        try {
            Class.forName("org.springframework.expression.spel.standard.SpelExpressionParser",
                    false, SpelScriptProcessorFactory.class.getClassLoader());
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * Constructs a {@code SpelScriptProcessorFactory} with default language name ({@value #LANGUAGE_NAME})
     * and the default binding variable name from {@link ScriptOptions#DEFAULT}.
     */
    public SpelScriptProcessorFactory() {
        this(LANGUAGE_NAME, ScriptOptions.DEFAULT.bindingsName());
    }

    /**
     * Constructs a {@code SpelScriptProcessorFactory} with the specified language and binding names.
     *
     * <p><strong>Note:</strong> the binding name is part of the expression author's contract —
     * expressions must reference the bindings variable under this exact name. All shipped XML
     * examples, documentation, and the default configuration use {@code #ctx}; choosing a
     * different name means every expression must use {@code #yourName} instead.
     *
     * @param languageName the language identifier for this factory; must not be empty
     * @param bindingName  the variable name under which the rule context's
     *                     {@link org.rulii.bind.Bindings} is exposed in SpEL expressions; must not be empty
     */
    public SpelScriptProcessorFactory(String languageName, String bindingName) {
        super();
        Assert.hasText(languageName, "languageName cannot be empty.");
        Assert.hasText(bindingName, "bindingName cannot be empty.");
        this.languageName = languageName;
        this.bindingName = bindingName;
    }

    /**
     * Returns whether the SpEL engine is usable: {@code false} when the (provided-scope)
     * {@code spring-expression} dependency is not on the classpath, so ServiceLoader
     * discovery skips this factory gracefully instead of failing later with a
     * {@link NoClassDefFoundError} during rule compilation.
     *
     * @return {@code true} if spring-expression is present
     */
    @Override
    public boolean isAvailable() {
        return SPEL_PRESENT;
    }

    /**
     * Returns the language identifier handled by this factory.
     *
     * @return the language name (e.g. {@code "el"})
     */
    @Override
    public String getLanguageName() {
        return languageName;
    }

    /**
     * Returns the variable name under which the rule context's bindings are exposed
     * within SpEL expressions.
     *
     * @return the binding variable name (e.g. {@code "ctx"})
     */
    @Override
    public String getBindingsName() {
        return bindingName;
    }

    /**
     * Creates a new {@link SpelScriptProcessor} for evaluating compiled SpEL scripts.
     *
     * @return a new {@link SpelScriptProcessor} instance
     */
    @Override
    public ScriptProcessor getScriptProcessor() {
        return new SpelScriptProcessor(languageName, bindingName);
    }

    /**
     * Creates a new {@link SpelScriptCompiler} for parsing SpEL expression strings.
     * The compiler stamps compiled scripts with this factory's language name so they
     * dispatch back to this factory's processor at evaluation time.
     *
     * @return a new {@link SpelScriptCompiler} instance
     */
    @Override
    public ScriptCompiler getScriptCompiler() {
        return new SpelScriptCompiler(languageName);
    }
}
