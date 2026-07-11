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

import org.rulii.rule.Rule;
import org.rulii.ruleset.RuleSet;
import org.rulii.ruleset.RuleSetBuilder;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.ClassUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring {@link FactoryBean} that constructs a {@link RuleSet} from configuration declared
 * in a {@code <rulii:ruleset>} XML element.
 *
 * <p>Spring resolves the {@code rules} property automatically: the parser registers each
 * inline or referenced rule as a separate bean definition and stores
 * {@link org.springframework.beans.factory.config.RuntimeBeanReference} entries in a
 * {@link org.springframework.beans.factory.support.ManagedList}; Spring injects the
 * resolved {@link Rule} instances before this factory bean's {@link #afterPropertiesSet()}
 * is called.
 *
 * @author Max Arulananthan
 * @since 1.0
 */
public class RuleSetFactoryBean implements FactoryBean<RuleSet<?>>, InitializingBean {

    private String name;
    private String description;
    private String defaultLanguage;
    private boolean validating = false;

    private List<InputParameterDefinition> params = new ArrayList<>();
    private ScriptExpression preCondition;
    private ScriptExpression initializer;
    private List<Rule> rules = new ArrayList<>();
    private ScriptExpression stopCondition;
    private ScriptExpression finalizer;
    private ScriptExpression resultExtractor;
    private ScriptExpression errorHandler;

    private RuleSet<?> ruleSet;

    public RuleSetFactoryBean() {
        super();
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void afterPropertiesSet() throws Exception {
        RuleSetBuilder builder = RuleSet.builder().with(name, description);

        for (InputParameterDefinition inputParameter : params) {
            Class type = ClassUtils.forName(inputParameter.getType(), Thread.currentThread().getContextClassLoader());

            if (inputParameter.getDefaultValueExpression() != null) {
                builder.param(inputParameter.getName(), type, inputParameter.getDefaultValueExpression().toFunction(defaultLanguage));
            } else {
                builder.param(inputParameter.getName(), type, inputParameter.isRequired());
            }
        }

        if (validating) builder.validating();

        if (preCondition != null) builder.preCondition(preCondition.toCondition(defaultLanguage));

        if (initializer != null) builder.initializer(initializer.toAction(defaultLanguage));

        if (rules != null) {
            rules.forEach(builder::rule);
        }

        if (stopCondition != null) builder.stopCondition(stopCondition.toCondition(defaultLanguage));

        if (finalizer != null) builder.finalizer(finalizer.toAction(defaultLanguage));

        if (resultExtractor != null) builder.resultExtractor(resultExtractor.toFunction(defaultLanguage));

        if (errorHandler != null) builder.errorHandler(errorHandler.toFunction(defaultLanguage));

        ruleSet = builder.build();
    }

    @Override
    public RuleSet<?> getObject() {
        return ruleSet;
    }

    @Override
    public Class<?> getObjectType() {
        return RuleSet.class;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }

    public void setValidating(boolean validating) {
        this.validating = validating;
    }

    public void setParams(List<InputParameterDefinition> params) {
        this.params = params != null ? params : new ArrayList<>();
    }

    public void setPreCondition(ScriptExpression preCondition) {
        this.preCondition = preCondition;
    }

    public void setInitializer(ScriptExpression initializer) {
        this.initializer = initializer;
    }

    public void setRules(List<Rule> rules) {
        this.rules = rules != null ? rules : new ArrayList<>();
    }

    public void setStopCondition(ScriptExpression stopCondition) {
        this.stopCondition = stopCondition;
    }

    public void setFinalizer(ScriptExpression finalizer) {
        this.finalizer = finalizer;
    }

    public void setResultExtractor(ScriptExpression resultExtractor) {
        this.resultExtractor = resultExtractor;
    }

    public void setErrorHandler(ScriptExpression errorHandler) {
        this.errorHandler = errorHandler;
    }

    // -------------------------------------------------------------------------

    /**
     * Parsed representation of a {@code <param>} element. Stored as a plain value on the
     * {@link RuleSetFactoryBean}'s bean definition; type resolution is deferred to
     * {@link #afterPropertiesSet()}.
     */
    public static class InputParameterDefinition {

        private final String name;
        private final String type;
        private final boolean required;
        private final ScriptExpression defaultValueExpression;

        public InputParameterDefinition(String name, String type, boolean required, ScriptExpression defaultValueExpression) {
            super();
            this.name = name;
            this.type = type;
            this.required = required;
            this.defaultValueExpression = defaultValueExpression;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public boolean isRequired() {
            return required;
        }

        public ScriptExpression getDefaultValueExpression() {
            return defaultValueExpression;
        }
    }
}
