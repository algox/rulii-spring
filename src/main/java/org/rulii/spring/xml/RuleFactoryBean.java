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

import org.rulii.model.action.Action;
import org.rulii.model.condition.Condition;
import org.rulii.rule.Rule;
import org.rulii.script.Script;
import org.rulii.script.ScriptProcessorManager;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring {@link FactoryBean} that constructs a {@link Rule} from script-based conditions
 * and actions declared in a {@code <rulii:rule>} XML element.
 *
 * <p>Scripts are compiled via the Spring-managed {@link ScriptProcessorManager} bean
 * (injected as a dependency) and wrapped using the standard rulii
 * {@code Condition.builder().build(script)} / {@code Action.builder().build(script)} API.
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

    public RuleFactoryBean() {}

    @Override
    public void afterPropertiesSet() {
        var builder = Rule.builder().name(name, description);

        if (preCondition != null) builder.preCondition(buildCondition(preCondition));

        if (condition != null) builder.given(buildCondition(condition));

        for (ScriptExpression expr : thenActions) {
            builder.then(buildAction(expr));
        }

        if (otherwiseAction != null) builder.otherwise(buildAction(otherwiseAction));

        rule = builder.build();
    }

    private Condition buildCondition(ScriptExpression expr) {
        return Condition.builder().build(
                Script.builder().build(expr.resolveLanguage(defaultLanguage), expr.getExpression()));
    }

    private Action buildAction(ScriptExpression expr) {
        return Action.builder().build(
                Script.builder().build(expr.resolveLanguage(defaultLanguage), expr.getExpression()));
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
