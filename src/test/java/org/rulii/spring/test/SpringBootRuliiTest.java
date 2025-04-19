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
package org.rulii.spring.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.rulii.bind.match.BindingMatchingStrategy;
import org.rulii.bind.match.ParameterResolver;
import org.rulii.context.RuleContext;
import org.rulii.context.RuleContextOptions;
import org.rulii.convert.ConverterRegistry;
import org.rulii.registry.RuleRegistry;
import org.rulii.rule.Rule;
import org.rulii.spring.registry.SpringRuleRegistry;
import org.rulii.text.MessageFormatter;
import org.rulii.text.MessageResolver;
import org.rulii.util.reflect.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * This class represents a Spring Boot test class for Rulii framework. It performs various tests related to rule context and rule registry components.
 *
 * @author Max Arulananthan
 * @since 1.0
 *
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestApplication.class)
public class SpringBootRuliiTest {

    @Autowired
    private BindingMatchingStrategy bindingMatchingStrategy;
    @Autowired
    private ParameterResolver parameterResolver;
    @Autowired
    private MessageFormatter messageFormatter;
    @Autowired
    private MessageResolver messageResolver;
    @Autowired
    private ObjectFactory objectFactory;
    @Autowired
    private ConverterRegistry converterRegistry;
    @Autowired
    private RuleRegistry ruleRegistry;
    @Autowired
    private RuleContextOptions ruleContextOptions;
    @Autowired
    private Rule testRule1;
    @Autowired
    private Rule testRule12;
    @Autowired
    private Set<Rule> rules;

    public SpringBootRuliiTest() {
        super();
    }

    @Test
    public void test1() {
        assertNotNull(bindingMatchingStrategy);
        assertNotNull(parameterResolver);
        assertNotNull(messageFormatter);
        assertNotNull(messageResolver);
        assertNotNull(objectFactory);
        assertNotNull(converterRegistry);
        assertNotNull(ruleRegistry);
        assertNotNull(ruleContextOptions);
    }

    @Test
    public void test2() {
        RuleContext context = RuleContext.builder().with(ruleContextOptions).build();
        assertNotNull(context);
        assertEquals(context.getConverterRegistry(), converterRegistry);
        assertEquals(context.getMatchingStrategy(), bindingMatchingStrategy);
        assertEquals(context.getMessageFormatter(), messageFormatter);
        assertEquals(context.getMessageResolver(), messageResolver);
        assertEquals(context.getObjectFactory(), objectFactory);
        //assertEquals(context.getObjectFactory().getClass(), SpringObjectFactory.class);
        assertEquals(context.getParameterResolver(), parameterResolver);
        assertEquals(ruleRegistry.getClass(), SpringRuleRegistry.class);
    }

    @Test
    public void test3() {
        assertNotNull(testRule1);
        assertNotNull(testRule12);
        assertEquals(rules.size(), 7);
    }
}
