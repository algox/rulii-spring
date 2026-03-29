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

import org.rulii.script.BuildScriptException;
import org.rulii.script.Script;
import org.rulii.script.ScriptCompiler;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * A {@link ScriptCompiler} implementation that parses Spring Expression Language (SpEL)
 * expressions into reusable {@link SpelScript} instances.
 *
 * <p>Parsing is performed once per compile call using a shared {@link SpelExpressionParser}.
 * The resulting {@link SpelScript} holds the pre-parsed {@link org.springframework.expression.Expression}
 * so that evaluation does not repeat the parsing overhead.</p>
 *
 * <p>This compiler is registered automatically via the {@link SpelScriptProcessorFactory}
 * and supports the {@code "el"} language identifier.</p>
 *
 * @author Max Arulananthan
 * @since 1.1
 * @see SpelScript
 * @see SpelScriptProcessorFactory
 */
public class SpelScriptCompiler implements ScriptCompiler {

    private final SpelExpressionParser parser = new SpelExpressionParser();

    /**
     * Constructs a new {@code SpelScriptCompiler}.
     */
    public SpelScriptCompiler() {
        super();
    }

    /**
     * Returns the language name this compiler handles.
     *
     * @return {@code "el"} (the Spring Expression Language identifier)
     */
    @Override
    public String getLanguageName() {
        return SpelScriptProcessorFactory.LANGUAGE_NAME;
    }


    @Override
    public <T> Script<T> compile(String script, Class<?> resultType) {
        try {
            return new SpelScript<>(getLanguageName(), script, resultType, parser.parseExpression(script));
        } catch (Exception e) {
            throw new BuildScriptException(script, e.getMessage(), e);
        }
    }
}
