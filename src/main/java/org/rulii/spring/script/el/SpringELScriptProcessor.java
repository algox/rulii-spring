package org.rulii.spring.script.el;

import org.rulii.context.RuleContext;
import org.rulii.script.*;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.Assert;

import java.util.*;

public class SpringELScriptProcessor implements ScriptProcessor {

    private static final Map<Script<?>, Expression> COMPILED_EXPRESSIONS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final List<PropertyAccessor> PROPERTY_ACCESSORS = List.of(new BindingAccessor());

    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final String languageName;
    private final String bindingName;

    public SpringELScriptProcessor(String languageName, String bindingName) {
        super();
        Assert.hasText(languageName, "languageName cannot be empty.");
        Assert.hasText(bindingName, "bindingName cannot be empty.");
        this.languageName = languageName;
        this.bindingName = bindingName;
    }

    @Override
    public String getLanguageName() {
        return languageName;
    }

    @Override
    public String getBindingName() {
        return bindingName;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T evaluate(Script<T> script, RuleContext ruleContext) {
        EvaluationContext evalContext = buildContext(ruleContext);
        Expression expression = getCompiledScript(script);

        try {
            // Add root object ? as the Map
            return (T) expression.getValue(evalContext);
        } catch (Exception e) {
            throw new EvaluationException(script.getScript(), e.getMessage(), e);
        }
    }

    protected EvaluationContext buildContext(RuleContext ruleContext) {
        Assert.notNull(ruleContext, "ruleContext cannot be null.");

        StandardEvaluationContext result = new StandardEvaluationContext();
        result.setPropertyAccessors(PROPERTY_ACCESSORS);

        Map<String, Object> vars = new HashMap<>();
        vars.put(getBindingName(), ruleContext.getBindings());

        result.setVariables(vars);

        return result;
    }

    protected Expression getCompiledScript(Script<?> script) {
        Expression cached = COMPILED_EXPRESSIONS.get(script);
        if (cached != null) return cached;
        Expression compiled = compile(script);
        COMPILED_EXPRESSIONS.put(script, compiled);
        return compiled;
    }

    protected Expression compile(Script<?> script) {
        try {
            return parser.parseExpression(script.getScript());
        } catch (Exception e) {
            throw new BuildScriptException(script.getScript(), e.getMessage(), e);
        }
    }
}
