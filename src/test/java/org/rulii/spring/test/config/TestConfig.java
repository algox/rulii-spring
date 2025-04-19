/*
 * This software is licensed under the Apache 2 license, quoted below.
 *
 * Copyright (c) 1999-2025, Algorithmx Inc.
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
package org.rulii.spring.test.config;

import org.rulii.model.action.Actions;
import org.rulii.model.condition.Conditions;
import org.rulii.rule.Rule;
import org.rulii.ruleset.RuleSet;
import org.rulii.spring.RuleBeans;
import org.rulii.spring.annotation.RuleScan;
import org.rulii.spring.test.rules.seta.TestRule1;
import org.rulii.spring.test.rules.seta.TestRule2;
import org.rulii.spring.test.rules.seta.TestRule3;
import org.rulii.util.reflect.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for setting up test rules.
 *
 * @author Max Arulananthan
 * @since 1.0
 *
 */
@Configuration
@RuleScan(scanBasePackages = "org.rulii.spring.test")
public class TestConfig {

    @Autowired
    private ObjectFactory objectFactory;
    @Autowired
    private RuleBeans ruleBeans;

    public TestConfig() {
        super();
    }

    @Bean("testRule55")
    public Rule testRule55() {
        return Rule.builder()
                .with(TestRule1.class, objectFactory)
                .build();
    }

    @Bean("testRule4")
    public Rule testRule4() {
        return Rule.builder()
                .name("TestRule4")
                .given(Conditions.TRUE())
                .then(Actions.EMPTY_ACTION())
                .build();
    }

    @Bean
    public RuleSet<?> testRuleSet() {
        return RuleSet.builder()
                .with("TestRuleSet1")
                .rule(Rule.builder().build(TestRule1.class, objectFactory))
                .rule(Rule.builder().build(TestRule2.class))
                .rule(ruleBeans.getRule(TestRule3.class))
                .rule(ruleBeans.getRule("testRule4"))
                .build();
    }
}
