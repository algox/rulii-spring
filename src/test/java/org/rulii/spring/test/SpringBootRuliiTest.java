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
import org.rulii.bind.Bindings;
import org.rulii.bind.match.BindingMatchingStrategy;
import org.rulii.bind.match.ParameterResolver;
import org.rulii.context.RuleContext;
import org.rulii.context.RuleContextOptions;
import org.rulii.convert.Converter;
import org.rulii.convert.ConverterRegistry;
import org.rulii.convert.text.TextToCurrencyConverter;
import org.rulii.model.UnrulyException;
import org.rulii.registry.RuleRegistry;
import org.rulii.rule.Rule;
import org.rulii.rule.RuleResult;
import org.rulii.ruleset.RuleSet;
import org.rulii.spring.convert.SpringConverterAdapter;
import org.rulii.spring.factory.SpringObjectFactory;
import org.rulii.spring.registry.SpringRuleRegistry;
import org.rulii.spring.test.model.Person;
import org.rulii.spring.test.model.PersonConverter;
import org.rulii.spring.test.rules.seta.TestRule1;
import org.rulii.spring.test.rules.seta.TestRule2;
import org.rulii.spring.test.rules.setb.TestRule12;
import org.rulii.text.MessageFormatter;
import org.rulii.text.MessageResolver;
import org.rulii.util.reflect.ObjectFactory;
import org.rulii.validation.RuleViolations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.time.Month;
import java.util.Currency;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This class represents a Spring Boot test class for Rulii framework. It performs various tests related to
 * rule context and rule registry components.
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
    private Person person;
    @Autowired
    private Rule testRule1;
    @Autowired
    private Rule testRule2;
    @Autowired
    private Rule testRule3;
    @Autowired
    private Rule testRule12;
    @Autowired
    private Rule consistentDateRule;
    @Autowired
    private List<Rule> rules;
    @Autowired
    private List<RuleSet<?>> ruleSets;

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
        assertEquals(context.getObjectFactory().getClass(), SpringObjectFactory.class);
        assertEquals(context.getParameterResolver(), parameterResolver);
        assertEquals(ruleRegistry.getClass(), SpringRuleRegistry.class);
    }

    @Test
    public void test3() {
        assertNotNull(person);
        assertNotNull(testRule1);
        assertNotNull(testRule12);
        assertEquals(rules.size(), 8);
    }

    @Test
    public void test4() {
        Converter<String, Currency> currencyConverter = converterRegistry.find(String.class, Currency.class);
        assertNotNull(currencyConverter);
        assertEquals(currencyConverter.getClass(), TextToCurrencyConverter.class);
        Currency currency = currencyConverter.convert("CAD", Currency.class);
        assertEquals(currency.getCurrencyCode(), "CAD");
    }

    @Test
    public void test5() {
        Converter<String, Pattern> regexConverter = converterRegistry.find(String.class, Pattern.class);
        assertNotNull(regexConverter);
        assertEquals(regexConverter.getClass(), SpringConverterAdapter.class);
        Pattern pattern = regexConverter.convert("test*", Pattern.class);
        assertEquals(pattern.getClass(), Pattern.class);
    }

    @Test
    public void test6() {
        Converter<String, Person> converter = converterRegistry.find(String.class, Person.class);
        assertNotNull(converter);
        assertEquals(converter.getClass(), PersonConverter.class);
        Person person =converter.convert("a,b,25", Person.class);
        assertEquals(person, new Person("a", "b", 25));
    }

    @Test
    public void test7() {
        TestRule1 rule = objectFactory.createRule(TestRule1.class);
        assertNotNull(rule);
        assertEquals(rule.getPerson(), person);
    }

    @Test
    public void test8() {
        TestRule2 rule = objectFactory.createRule(TestRule2.class);
        assertNotNull(rule);
        assertEquals(rule.getExternalValue(), "100");
    }

    @Test
    public void test9() {
        assertNotNull(ruleRegistry);
        assertEquals(testRule1, ruleRegistry.getRule("testRule1"));
    }

    @Test
    public void test10() {
        assertNotNull(ruleRegistry);
        assertThrows(UnrulyException.class, () ->
            assertEquals(testRule1, ruleRegistry.getRule(TestRule1.class))
        );
    }


    @Test
    public void test11() {
        assertEquals(testRule12, ruleRegistry.getRule("testRule12"));
    }

    @Test
    public void test12() {
        assertEquals(testRule12, ruleRegistry.getRule(TestRule12.class));
    }

    @Test
    public void test13() {
        assertEquals(ruleRegistry.getCount(), 9);
    }

    @Test
    public void test14() {
        assertFalse(ruleRegistry.isNameInUse("xxx"));
        assertFalse(ruleRegistry.isNameInUse("testRule13"));
    }

    @Test
    public void test15() {
        assertTrue(ruleRegistry.isNameInUse("testRule12"));
        assertTrue(ruleRegistry.isNameInUse("testRuleSet"));
    }

    @Test
    public void test16() {
        assertEquals(rules, ruleRegistry.getRules());
    }

    @Test
    public void test17() {
        assertEquals(ruleSets, ruleRegistry.getRuleSets());
    }

    @Test
    public void test18() {
        assertNotNull(ruleRegistry.getRuleSet("testRuleSet"));
    }

    @Test
    public void test19() {
        List<Rule> rules = ruleRegistry.getRulesInPackage("org.rulii.spring.test.rules.seta");
        assertEquals(rules.size(), 4);
        assertTrue(rules.contains(testRule1));
        assertTrue(rules.contains(testRule2));
    }

    @Test
    public void test20() {
        RuleSet<?> ruleSet = ruleRegistry.getRuleSet("testRuleSet");
        assertEquals(ruleSet.getRule(0).getClass(), testRule1.getClass());
        assertEquals(ruleSet.getRule(2).getClass(), testRule2.getClass());
        assertEquals(ruleSet.getRule(3).getClass(), testRule3.getClass());
    }

    @Test
    public void test21() {
        RuleSet<?> ruleSet = ruleRegistry.getRuleSet("testRuleSet");
        ruleSet.run();
    }

    @Test
    public void test22() {
        Bindings bindings = Bindings.builder().standard();
        bindings.bind("fromDate", LocalDate.now());
        bindings.bind("toDate", LocalDate.of(1980, Month.JANUARY, 1));
        bindings.bind("violations", new RuleViolations());

        RuleContext context = RuleContext.builder()
                .with(ruleContextOptions)
                .bindings(bindings)
                .build();

        // Run the Rule
        RuleResult result = consistentDateRule.run(context);
    }
}
