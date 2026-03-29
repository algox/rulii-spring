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

import org.rulii.script.AbstractScript;
import org.springframework.expression.Expression;
import org.springframework.util.Assert;

/**
 * A compiled Spring Expression Language (SpEL) script that wraps a pre-parsed
 * {@link Expression} for deferred evaluation within a Rulii {@link org.rulii.context.RuleContext}.
 *
 * <p>Instances are produced by {@link SpelScriptCompiler} and evaluated by
 * {@link SpelScriptProcessor}. The underlying expression is parsed once at compile
 * time and reused across multiple evaluations, making repeated execution efficient.</p>
 *
 * @param <T> the expected return type of the SpEL expression
 *
 * @author Max Arulananthan
 * @since 1.1
 * @see SpelScriptCompiler
 * @see SpelScriptProcessor
 */
public class SpelScript<T> extends AbstractScript<T> {

    private final Expression expression;

    public SpelScript(String languageName, String script, Class<?> returnType, Expression expression) {
        super(languageName, script, returnType);
        Assert.notNull(expression, "expression cannot be null.");
        this.expression = expression;
    }

    /**
     * Returns the pre-parsed SpEL {@link Expression} held by this script.
     *
     * @return the parsed expression; never {@code null}
     */
    public Expression getExpression() {
        return expression;
    }

    @Override
    public String toString() {
        return "SpelScript{" +
                "expression=" + expression +
                '}';
    }
}
