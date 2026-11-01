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

import org.junit.jupiter.api.Test;
import org.rulii.model.ResultType;
import org.rulii.model.UnrulyException;
import org.rulii.ruleflow.RuleFlow;
import org.rulii.ruleset.RuleSet;
import org.rulii.ruleset.RuleSetExecutionStatus;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The result type declared in XML: {@code type} on {@code <result-extractor>} / {@code <returning>}, or
 * {@code result-type} on {@code <ruleset>} / {@code <ruleflow>}; one or the other; the class must load;
 * a flow needs a returning expression. Rulesets and flows without a declaration resolve as rulii does.
 *
 * @author Max Arulananthan
 * @since 2.2
 */
class ResultTypeXmlTest {

    private static final String HEADER = """
            <?xml version="1.0" encoding="UTF-8"?>
            <beans xmlns="http://www.springframework.org/schema/beans"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xmlns:r="http://www.rulii.org/schema/rulii"
                   xsi:schemaLocation="
                       http://www.springframework.org/schema/beans
                       https://www.springframework.org/schema/beans/spring-beans.xsd
                       http://www.rulii.org/schema/rulii
                       https://www.rulii.org/spring/rulii-spring.xsd">
            """;

    private static Resource xml(String body) {
        return new ByteArrayResource((HEADER + body + "</beans>\n").getBytes(StandardCharsets.UTF_8),
                "result-type-test");
    }

    private static DefaultListableBeanFactory load(String body) {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        new XmlBeanDefinitionReader(factory).loadBeanDefinitions(xml(body));
        return factory;
    }

    private static <T extends Throwable> T findCause(Throwable t, Class<T> type) {
        Throwable current = t;

        while (current != null) {
            if (type.isInstance(current)) return type.cast(current);
            current = current.getCause();
        }

        return null;
    }

    // ------------------------------------------------------------------
    // Rulesets
    // ------------------------------------------------------------------

    @Test
    void rulesetWithoutADeclarationResolvesAsRulii() {
        DefaultListableBeanFactory factory = load("""
                <r:ruleset name="PlainSet"/>
                <r:ruleset name="ScriptSet">
                    <r:result-extractor>7</r:result-extractor>
                </r:ruleset>
                """);

        RuleSet<?> plain = factory.getBean("PlainSet", RuleSet.class);
        assertEquals(ResultType.byDefault(RuleSetExecutionStatus.class), plain.getDefinition().getResolvedResultType());

        RuleSet<?> script = factory.getBean("ScriptSet", RuleSet.class);
        assertEquals(ResultType.unknown(), script.getDefinition().getResolvedResultType(), "a script cannot declare its type");
        assertEquals(7, script.run());
    }

    @Test
    void rulesetTypeOnTheExtractorElement() {
        DefaultListableBeanFactory factory = load("""
                <r:ruleset name="TypedSet">
                    <r:result-extractor type="java.lang.Integer">7</r:result-extractor>
                </r:ruleset>
                """);

        RuleSet<?> ruleSet = factory.getBean("TypedSet", RuleSet.class);
        assertEquals(ResultType.declared(Integer.class), ruleSet.getDefinition().getResolvedResultType());
        assertEquals(Integer.class, ruleSet.getDefinition().getResultType());
        assertEquals(7, ruleSet.run());
    }

    @Test
    void rulesetTypeOnTheRulesetElement() {
        DefaultListableBeanFactory factory = load("""
                <r:ruleset name="TypedSet" result-type="java.lang.Number">
                    <r:result-extractor>7</r:result-extractor>
                </r:ruleset>
                <r:ruleset name="StatusSet" result-type="org.rulii.ruleset.RuleSetExecutionStatus"/>
                """);

        assertEquals(ResultType.declared(Number.class),
                factory.getBean("TypedSet", RuleSet.class).getDefinition().getResolvedResultType());
        assertEquals(ResultType.declared(RuleSetExecutionStatus.class),
                factory.getBean("StatusSet", RuleSet.class).getDefinition().getResolvedResultType(),
                "stated on its own, with the default extractor");
    }

    @Test
    void rulesetDeclaringBothFormsFailsAtParseTime() {
        BeanDefinitionParsingException ex = assertThrows(BeanDefinitionParsingException.class, () -> load("""
                <r:ruleset name="TwiceSet" result-type="java.lang.Integer">
                    <r:result-extractor type="java.lang.Integer">7</r:result-extractor>
                </r:ruleset>
                """));

        assertTrue(ex.getMessage().contains("<ruleset> declares the result type both as [result-type] and as [type] on <result-extractor>"),
                ex.getMessage());
    }

    @Test
    void rulesetWithAnUnknownClassFailsNamingTheRuleset() {
        DefaultListableBeanFactory factory = load("""
                <r:ruleset name="BadClassSet" result-type="com.example.NoSuchType"/>
                """);

        BeanCreationException ex = assertThrows(BeanCreationException.class, () -> factory.getBean("BadClassSet"));
        UnrulyException cause = findCause(ex, UnrulyException.class);
        assertNotNull(cause, "the failure should surface as UnrulyException");
        assertTrue(cause.getMessage().contains("com.example.NoSuchType"), cause.getMessage());
        assertTrue(cause.getMessage().contains("BadClassSet"), cause.getMessage());
    }

    // ------------------------------------------------------------------
    // Ruleflows
    // ------------------------------------------------------------------

    @Test
    void ruleflowWithoutADeclarationResolvesAsRulii() {
        DefaultListableBeanFactory factory = load("""
                <r:ruleflow name="ContextFlow">
                    <r:bind name="a" value="1"/>
                </r:ruleflow>
                <r:ruleflow name="ScriptFlow" returning="7"/>
                """);

        RuleFlow<?> context = factory.getBean("ContextFlow", RuleFlow.class);
        RuleFlow<?> script = factory.getBean("ScriptFlow", RuleFlow.class);
        assertEquals(ResultType.none(), context.getDefinition().getResolvedResultType());
        assertEquals(ResultType.unknown(), script.getDefinition().getResolvedResultType());
    }

    @Test
    void ruleflowTypeOnTheReturningElement() {
        DefaultListableBeanFactory factory = load("""
                <r:ruleflow name="TypedFlow">
                    <r:bind name="a">3</r:bind>
                    <r:returning type="java.lang.Integer">#ctx.a * 2</r:returning>
                </r:ruleflow>
                """);

        RuleFlow<?> flow = factory.getBean("TypedFlow", RuleFlow.class);
        assertEquals(ResultType.declared(Integer.class), flow.getDefinition().getResolvedResultType());
        assertEquals(6, flow.run());
    }

    @Test
    void ruleflowTypeOnTheRuleflowElementWithTheReturningAttribute() {
        DefaultListableBeanFactory factory = load("""
                <r:ruleflow name="TypedFlow" result-type="java.lang.Number" returning="7"/>
                """);

        RuleFlow<?> flow = factory.getBean("TypedFlow", RuleFlow.class);
        assertEquals(ResultType.declared(Number.class), flow.getDefinition().getResolvedResultType());
    }

    @Test
    void ruleflowDeclaringBothFormsFailsAtParseTime() {
        BeanDefinitionParsingException ex = assertThrows(BeanDefinitionParsingException.class, () -> load("""
                <r:ruleflow name="TwiceFlow" result-type="java.lang.Integer">
                    <r:returning type="java.lang.Integer">7</r:returning>
                </r:ruleflow>
                """));

        assertTrue(ex.getMessage().contains("<ruleflow> declares the result type both as [result-type] and as [type] on <returning>"),
                ex.getMessage());
    }

    @Test
    void ruleflowTypeWithoutAReturningExpressionFailsAtBuild() {
        DefaultListableBeanFactory factory = load("""
                <r:ruleflow name="NoReturningFlow" result-type="java.lang.Integer">
                    <r:bind name="a" value="1"/>
                </r:ruleflow>
                """);

        BeanCreationException ex = assertThrows(BeanCreationException.class, () -> factory.getBean("NoReturningFlow"));
        UnrulyException cause = findCause(ex, UnrulyException.class);
        assertNotNull(cause, "the build failure should surface as UnrulyException");
        assertTrue(cause.getMessage().contains("RuleFlow [NoReturningFlow] declares result type [Integer] but returns its RuleContext"),
                cause.getMessage());
    }

    @Test
    void ruleflowWithAnUnknownClassFailsNamingTheFlow() {
        DefaultListableBeanFactory factory = load("""
                <r:ruleflow name="BadClassFlow" result-type="com.example.NoSuchType" returning="7"/>
                """);

        BeanCreationException ex = assertThrows(BeanCreationException.class, () -> factory.getBean("BadClassFlow"));
        UnrulyException cause = findCause(ex, UnrulyException.class);
        assertNotNull(cause, "the failure should surface as UnrulyException");
        assertTrue(cause.getMessage().contains("com.example.NoSuchType"), cause.getMessage());
    }
}
