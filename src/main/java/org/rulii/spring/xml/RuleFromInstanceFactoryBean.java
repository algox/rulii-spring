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

/**
 * Spring {@link FactoryBean} that wraps an already-instantiated rule target object as a
 * {@link Rule}.
 *
 * <p>Used by {@code <rulii:class-ref>} parsing when constructor arguments or property
 * injections are present. Spring creates and fully wires the rule target instance (handling
 * all type conversion and bean references); this factory bean then delegates to
 * {@link Rule#builder()} to produce the finished {@link Rule}.
 *
 * @author Max Arulananthan
 * @since 1.0
 */
class RuleFromInstanceFactoryBean implements FactoryBean<Rule>, InitializingBean {

    private Object ruleInstance;
    private Rule rule;

    RuleFromInstanceFactoryBean() {}

    @Override
    public void afterPropertiesSet() {
        if (ruleInstance == null) throw new IllegalStateException("ruleInstance must not be null");
        rule = Rule.builder().build(ruleInstance);
    }

    public void setRuleInstance(Object ruleInstance) {
        this.ruleInstance = ruleInstance;
    }

    @Override
    public Rule getObject() {
        return rule;
    }

    @Override
    public Class<?> getObjectType() {
        return Rule.class;
    }
}
