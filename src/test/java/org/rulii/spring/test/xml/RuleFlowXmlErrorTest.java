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
import org.rulii.model.UnrulyException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Failure-path tests for {@code <r:ruleflow>} parsing and building: structural errors
 * report the XML source location at parse time; build errors name the flow.
 *
 * @author Max Arulananthan
 * @since 2.0
 */
class RuleFlowXmlErrorTest {

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
                "ruleflow-error-test");
    }

    @Test
    void runWithTwoTargetsFailsAtParseTime() {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);

        BeanDefinitionParsingException ex = assertThrows(BeanDefinitionParsingException.class,
                () -> reader.loadBeanDefinitions(xml("""
                        <r:ruleflow name="BadTargets">
                            <r:run bean-ref="someBean" name="alsoAName"/>
                        </r:ruleflow>
                        """)));

        assertTrue(ex.getMessage().contains("exactly one target"),
                "error should explain the target contract, but was: " + ex.getMessage());
    }

    @Test
    void bindWithNoSourceFailsAtParseTime() {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);

        BeanDefinitionParsingException ex = assertThrows(BeanDefinitionParsingException.class,
                () -> reader.loadBeanDefinitions(xml("""
                        <r:ruleflow name="BadBind">
                            <r:bind name="x"/>
                        </r:ruleflow>
                        """)));

        assertTrue(ex.getMessage().contains("exactly one of"),
                "error should explain the bind source contract, but was: " + ex.getMessage());
    }

    @Test
    void unknownExceptionClassFailsAtBuildNamingTheFlow() {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
        reader.loadBeanDefinitions(xml("""
                <r:ruleflow name="BadHandlerFlow">
                    <r:execute>
                        1 + 1
                        <r:on-exception exception="com.example.NoSuchException">
                            <r:exit/>
                        </r:on-exception>
                    </r:execute>
                </r:ruleflow>
                """));

        BeanCreationException ex = assertThrows(BeanCreationException.class,
                () -> factory.getBean("BadHandlerFlow"));

        UnrulyException cause = findCause(ex, UnrulyException.class);
        assertNotNull(cause, "the build failure should surface as UnrulyException");
        assertTrue(cause.getMessage().contains("BadHandlerFlow"),
                "the failure should name the flow, but was: " + cause.getMessage());
        assertTrue(cause.getMessage().contains("com.example.NoSuchException"),
                "the failure should name the missing class, but was: " + cause.getMessage());
    }

    private static <T extends Throwable> T findCause(Throwable t, Class<T> type) {
        while (t != null) {
            if (type.isInstance(t)) return type.cast(t);
            t = t.getCause();
        }
        return null;
    }
}
