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
package org.rulii.spring.xml;

import org.rulii.model.UnrulyException;
import org.rulii.model.condition.Condition;
import org.rulii.rule.Rule;
import org.rulii.rule.RuleDefinition;
import org.rulii.script.Script;
import org.rulii.script.ScriptProcessorManager;
import org.rulii.validation.Severity;
import org.rulii.validation.ValidationRuleBuilder;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.StringUtils;

/**
 * Spring {@link FactoryBean} that constructs a validation {@link Rule} from a
 * {@code <rulii:validationRule>} XML element.
 *
 * <p>The {@code given} condition is compiled from a script expression via the Spring-managed
 * {@link ScriptProcessorManager}. All validation metadata (errorCode, severity, messages) are
 * passed directly to the rulii {@code ValidationRuleBuilder}.
 *
 * @author Max Arulananthan
 * @since 1.0
 */
public class ValidationRuleFactoryBean implements FactoryBean<Rule>, InitializingBean {

    private String name;
    private String description;
    private String defaultLanguage;

    private ScriptExpression condition;
    private String errorCode;
    private String severity;
    private String errorMessage;
    private String defaultMessage;

    private Rule rule;

    public ValidationRuleFactoryBean() {}

    @Override
    public void afterPropertiesSet() {
        if (condition == null) throw new UnrulyException("ValidationRule '" + name + "' must have a <given> condition.");

        final String ruleName = this.name;
        ValidationRuleBuilder builder = new ValidationRuleBuilder(ruleName, buildCondition(condition));

        builder.errorCode(errorCode);
        if (StringUtils.hasText(description)) builder.description(description);
        if (StringUtils.hasText(severity)) builder.severity(Severity.valueOf(severity.toUpperCase()));
        if (StringUtils.hasText(errorMessage)) builder.errorMessage(errorMessage);
        if (StringUtils.hasText(defaultMessage)) builder.defaultMessage(defaultMessage);

        rule = builder.build();
    }

    private Condition buildCondition(ScriptExpression expr) {
        return Condition.builder().build(
                Script.builder().build(expr.resolveLanguage(defaultLanguage), expr.getExpression()));
    }

    @Override
    public Rule getObject() { return rule; }

    @Override
    public Class<?> getObjectType() { return Rule.class; }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }

    public void setCondition(ScriptExpression condition) {
        this.condition = condition;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setDefaultMessage(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }
}
