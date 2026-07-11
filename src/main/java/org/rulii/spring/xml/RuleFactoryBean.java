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
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring {@link FactoryBean} that constructs a {@link Rule} from script-based conditions
 * and actions declared in a {@code <rulii:rule>} XML element.
 *
 * <p>Scripts are compiled through the standard rulii {@code Script.builder()} pipeline via
 * {@link ScriptExpression#toCondition} / {@link ScriptExpression#toAction}.
 *
 * @author Max Arulananthan
 * @since 1.0
 */
public class RuleFactoryBean implements FactoryBean<Rule>, InitializingBean {

    private String name;
    private String description;
    private String defaultLanguage;

    private ScriptExpression preCondition;
    private ScriptExpression condition;
    private List<ScriptExpression> thenActions = new ArrayList<>();
    private ScriptExpression otherwiseAction;

    private Rule rule;

    public RuleFactoryBean() {
        super();
    }

    @Override
    public void afterPropertiesSet() {
        var builder = Rule.builder().name(name, description);

        if (preCondition != null) builder.preCondition(preCondition.toCondition(defaultLanguage));

        if (condition != null) builder.given(condition.toCondition(defaultLanguage));

        for (ScriptExpression expr : thenActions) {
            builder.then(expr.toAction(defaultLanguage));
        }

        if (otherwiseAction != null) builder.otherwise(otherwiseAction.toAction(defaultLanguage));

        rule = builder.build();
    }

    @Override
    public Rule getObject() {
        return rule;
    }

    @Override
    public Class<?> getObjectType() {
        return Rule.class;
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

    public void setPreCondition(ScriptExpression preCondition) {
        this.preCondition = preCondition;
    }

    public void setCondition(ScriptExpression condition) {
        this.condition = condition;
    }

    public void setThenActions(List<ScriptExpression> thenActions) {
        this.thenActions = thenActions != null ? thenActions : new ArrayList<>();
    }

    public void setOtherwiseAction(ScriptExpression otherwiseAction) {
        this.otherwiseAction = otherwiseAction;
    }
}
