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
package org.rulii.spring.test.xml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rulii.context.RuleContext;
import org.rulii.rule.Rule;
import org.rulii.ruleflow.RuleFlow;
import org.rulii.ruleset.RuleSet;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the terse attribute form of condition, action, and function declarations —
 * {@code given}/{@code pre-condition}/{@code then}/{@code otherwise} on rule,
 * {@code given} on validationRule,
 * {@code pre-condition}/{@code stop-condition}/{@code initializer}/{@code finalizer} on ruleset,
 * {@code condition} on ruleflow when, {@code source}/{@code stop-condition} on for-each,
 * {@code finalizer}/{@code returning} on ruleflow —
 * behaving identically to the child-element form, and that declaring both
 * forms fails at parse time.
 *
 * @author Max Arulananthan
 * @since 2.0
 */
class AttributeConditionXmlTest {

    private DefaultListableBeanFactory factory;

    @BeforeEach
    void setUp() {
        factory = new DefaultListableBeanFactory();
        new XmlBeanDefinitionReader(factory)
                .loadBeanDefinitions(new ClassPathResource("rules/condition-attr-test.xml"));
    }

    private RuleContext ctx() {
        return RuleContext.builder().standard().build();
    }

    @Test
    void ruleWithGivenAndPreConditionAttributes() {
        Rule rule = factory.getBean("AttrGivenRule", Rule.class);

        RuleContext adult = ctx();
        adult.getBindings().bind("age", 18);
        assertTrue(rule.isTrue(adult));

        RuleContext minor = ctx();
        minor.getBindings().bind("age", 17);
        assertFalse(rule.isTrue(minor));
    }

    @Test
    void validationRuleWithGivenAttribute() {
        Rule rule = factory.getBean("AttrValidationRule", Rule.class);

        RuleContext positive = ctx();
        positive.getBindings().bind("value", 1);
        assertTrue(rule.isTrue(positive));

        RuleContext zero = ctx();
        zero.getBindings().bind("value", 0);
        assertFalse(rule.isTrue(zero));
    }

    @Test
    void rulesetWithConditionAttributes() {
        RuleSet<?> ruleSet = factory.getBean("AttrRuleSet", RuleSet.class);

        RuleContext go = ctx();
        go.getBindings().bind("go", true);
        go.getBindings().bind("stop", false);
        go.getBindings().bind("ran", false);
        ruleSet.run(go);
        assertEquals(Boolean.TRUE, go.getBindings().getValue("ran"));

        RuleContext held = ctx();
        held.getBindings().bind("go", false);
        held.getBindings().bind("stop", false);
        held.getBindings().bind("ran", false);
        ruleSet.run(held);
        assertEquals(Boolean.FALSE, held.getBindings().getValue("ran"),
                "the pre-condition attribute must gate the ruleset");
    }

    @Test
    void ruleflowWithConditionAttributes() {
        RuleFlow<?> flow = factory.getBean("AttrFlow", RuleFlow.class);

        RuleContext approvedCtx = ctx();
        approvedCtx.getBindings().bind("total", 150);
        approvedCtx.getBindings().bind("approved", false);
        approvedCtx.getBindings().bind("sum", 0);
        approvedCtx.getBindings().bind("stopLoop", false);
        flow.run(approvedCtx);

        assertEquals(Boolean.TRUE, approvedCtx.getBindings().getValue("approved"),
                "the when condition attribute must select the then-branch");
        assertEquals(6, (Integer) approvedCtx.getBindings().getValue("sum"),
                "the for-each with stop-condition attribute must iterate fully");

        RuleContext rejectedCtx = ctx();
        rejectedCtx.getBindings().bind("total", 50);
        rejectedCtx.getBindings().bind("approved", false);
        rejectedCtx.getBindings().bind("sum", 0);
        rejectedCtx.getBindings().bind("stopLoop", false);
        flow.run(rejectedCtx);

        assertEquals(Boolean.FALSE, rejectedCtx.getBindings().getValue("approved"));
    }

    @Test
    void ruleWithThenAndOtherwiseAttributes() {
        Rule rule = factory.getBean("AttrActionRule", Rule.class);

        RuleContext approved = ctx();
        approved.getBindings().bind("total", 150);
        approved.getBindings().bind("approved", false);
        rule.run(approved);
        assertEquals(Boolean.TRUE, approved.getBindings().getValue("approved"),
                "the then attribute must execute on a passing condition");

        RuleContext rejected = ctx();
        rejected.getBindings().bind("total", 50);
        rejected.getBindings().bind("approved", true);
        rule.run(rejected);
        assertEquals(Boolean.FALSE, rejected.getBindings().getValue("approved"),
                "the otherwise attribute must execute on a failing condition");
    }

    @Test
    void rulesetWithLifecycleAttributes() {
        RuleSet<?> ruleSet = factory.getBean("AttrLifecycleRuleSet", RuleSet.class);

        RuleContext ctx = ctx();
        ctx.getBindings().bind("trace", "");
        ctx.getBindings().bind("ran", false);
        ruleSet.run(ctx);

        assertEquals("IF", ctx.getBindings().getValue("trace"),
                "initializer and finalizer attributes must run in order");
        assertEquals(Boolean.TRUE, ctx.getBindings().getValue("ran"));
    }

    @Test
    void ruleflowWithFinalizerAndReturningAttributes() {
        RuleFlow<?> flow = factory.getBean("AttrReturningFlow", RuleFlow.class);

        RuleContext ctx = ctx();
        ctx.getBindings().bind("v", 5);
        ctx.getBindings().bind("flowDone", false);

        Object result = flow.run(ctx);

        assertEquals(10, result, "the returning attribute must extract the flow result");
        assertEquals(Boolean.TRUE, ctx.getBindings().getValue("flowDone"),
                "the finalizer attribute must run");
    }

    @Test
    void declaringThenBothWaysFailsAtParseTime() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xmlns:r="http://www.rulii.org/schema/rulii"
                       xsi:schemaLocation="
                           http://www.springframework.org/schema/beans
                           https://www.springframework.org/schema/beans/spring-beans.xsd
                           http://www.rulii.org/schema/rulii
                           https://www.rulii.org/spring/rulii-spring.xsd">
                    <r:rule name="DupThenRule" given="true" then="#ctx.x = 1">
                        <r:then>#ctx.x = 2</r:then>
                    </r:rule>
                </beans>
                """;

        DefaultListableBeanFactory freshFactory = new DefaultListableBeanFactory();
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(freshFactory);

        BeanDefinitionParsingException ex = assertThrows(BeanDefinitionParsingException.class,
                () -> reader.loadBeanDefinitions(new ByteArrayResource(
                        xml.getBytes(StandardCharsets.UTF_8), "both-then-forms-test")));

        assertTrue(ex.getMessage().contains("[then] both as an attribute"),
                "error should explain the exclusivity, but was: " + ex.getMessage());
    }

    @Test
    void declaringBothFormsFailsAtParseTime() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xmlns:r="http://www.rulii.org/schema/rulii"
                       xsi:schemaLocation="
                           http://www.springframework.org/schema/beans
                           https://www.springframework.org/schema/beans/spring-beans.xsd
                           http://www.rulii.org/schema/rulii
                           https://www.rulii.org/spring/rulii-spring.xsd">
                    <r:rule name="DupConditionRule" given="true">
                        <r:given>true</r:given>
                    </r:rule>
                </beans>
                """;

        DefaultListableBeanFactory freshFactory = new DefaultListableBeanFactory();
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(freshFactory);

        BeanDefinitionParsingException ex = assertThrows(BeanDefinitionParsingException.class,
                () -> reader.loadBeanDefinitions(new ByteArrayResource(
                        xml.getBytes(StandardCharsets.UTF_8), "both-forms-test")));

        assertTrue(ex.getMessage().contains("both as an attribute and as a child element"),
                "error should explain the exclusivity, but was: " + ex.getMessage());
    }
}
