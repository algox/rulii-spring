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
import org.rulii.rule.Rule;
import org.rulii.model.SourceDefinition;
import org.rulii.validation.Severity;
import org.rulii.validation.ValidationRuleBuilder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Spring {@link FactoryBean} that constructs a validation {@link Rule} from a
 * {@code <rulii:validationRule>} XML element.
 *
 * <p>The {@code given} condition is compiled through the standard rulii
 * {@code Script.builder()} pipeline via {@link ScriptExpression#toCondition}. All validation
 * metadata (errorCode, severity, messages) are passed directly to the rulii
 * {@code ValidationRuleBuilder}.
 *
 * @author Max Arulananthan
 * @since 1.0
 */
public class ValidationRuleFactoryBean implements FactoryBean<Rule>, InitializingBean, BeanNameAware, BeanFactoryAware {

    private String name;
    private String description;
    private String category;
    private List<String> tags = new ArrayList<>();
    private String defaultLanguage;

    private ScriptExpression condition;
    private String errorCode;
    private String severity;
    private String errorMessage;
    private String defaultMessage;

    private Rule rule;
    private String beanName;
    private BeanFactory beanFactory;

    public ValidationRuleFactoryBean() {
        super();
    }

    @Override
    public void afterPropertiesSet() {
        if (condition == null) throw new UnrulyException("ValidationRule '" + name + "' must have a <given> condition.");

        ValidationRuleBuilder builder = new ValidationRuleBuilder(name, condition.toCondition(defaultLanguage));

        builder.errorCode(errorCode);
        if (StringUtils.hasText(description)) builder.description(description);
        builder.category(category);
        builder.tags(tags);
        if (StringUtils.hasText(severity)) builder.severity(Severity.valueOf(severity.toUpperCase(Locale.ROOT)));
        if (StringUtils.hasText(errorMessage)) builder.errorMessage(errorMessage);
        if (StringUtils.hasText(defaultMessage)) builder.defaultMessage(defaultMessage);

        // The bean definition knows the XML file and line; the artifact reports them as its source.
        SourceDefinition source = BeanSources.sourceOf(beanFactory, beanName);
        if (source != null) builder.source(source);
        rule = builder.build();
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

    public void setCategory(String category) {
        this.category = category;
    }

    public void setTags(List<String> tags) {
        this.tags = tags != null ? tags : new ArrayList<>();
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
    @Override
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}
