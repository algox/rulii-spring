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
import org.rulii.context.RuleContext;
import org.rulii.model.UnrulyException;
import org.rulii.ruleflow.RuleFlow;
import org.rulii.ruleflow.RuleFlowExecutionContext;
import org.rulii.ruleflow.command.ContainerCommand;
import org.rulii.ruleflow.command.RuleFlowCommand;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies custom command support in {@code <r:ruleflow>}: leaf {@code RuleFlowCommand}
 * beans via {@code <r:command ref="..."/>}, custom {@link ContainerCommand} beans
 * receiving a nested command body, and the failure paths (body on a leaf command;
 * one container instance reused for several bodies).
 *
 * @author Max Arulananthan
 * @since 2.0
 */
class RuleFlowContainerXmlTest {

    /** Container command executing its body a fixed number of times. */
    public static class RepeatCommand extends ContainerCommand {

        private final int times;

        public RepeatCommand(int times) {
            super();
            this.times = times;
        }

        @Override
        public void execute(RuleFlowExecutionContext ctx) {
            for (int i = 0; i < times; i++) {
                for (RuleFlowCommand command : getBody()) {
                    command.execute(ctx);
                }
            }
        }
    }

    /** Leaf command writing a marker binding. */
    public static class MarkerCommand implements RuleFlowCommand {

        public MarkerCommand() {
            super();
        }

        @Override
        public void execute(RuleFlowExecutionContext ctx) {
            ctx.getRuleContext().getBindings().setValueOrBind("marker", true);
        }
    }

    private DefaultListableBeanFactory loadFixture() {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        factory.registerBeanDefinition("markerCommand",
                BeanDefinitionBuilder.genericBeanDefinition(MarkerCommand.class).getBeanDefinition());
        factory.registerBeanDefinition("repeatCommand",
                BeanDefinitionBuilder.genericBeanDefinition(RepeatCommand.class)
                        .addConstructorArgValue(2).setScope("prototype").getBeanDefinition());

        new XmlBeanDefinitionReader(factory)
                .loadBeanDefinitions(new ClassPathResource("rules/ruleflow-container-test.xml"));
        return factory;
    }

    @Test
    void containerCommandExecutesItsParsedBody() {
        RuleFlow<?> flow = loadFixture().getBean("ContainerFlow", RuleFlow.class);

        RuleContext ctx = RuleContext.builder().standard().build();
        ctx.getBindings().bind("marker", false);
        ctx.getBindings().bind("count", 0);
        ctx.getBindings().bind("sawTwo", false);

        flow.run(ctx);

        assertEquals(Boolean.TRUE, ctx.getBindings().getValue("marker"), "the leaf command must have run");
        assertEquals(2, (Integer) ctx.getBindings().getValue("count"),
                "RepeatCommand(2) must execute its body twice");
        assertEquals(Boolean.TRUE, ctx.getBindings().getValue("sawTwo"),
                "nested containers (when inside command) must work inside the custom body");
    }

    @Test
    void bodyOnLeafCommandFailsAtBuildNamingTheBean() {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        factory.registerBeanDefinition("markerCommand",
                BeanDefinitionBuilder.genericBeanDefinition(MarkerCommand.class).getBeanDefinition());

        new XmlBeanDefinitionReader(factory).loadBeanDefinitions(flowXml("""
                <r:ruleflow name="BadLeafFlow">
                    <r:command ref="markerCommand">
                        <r:exit/>
                    </r:command>
                </r:ruleflow>
                """));

        BeanCreationException ex = assertThrows(BeanCreationException.class,
                () -> factory.getBean("BadLeafFlow"));

        UnrulyException cause = findCause(ex);
        assertNotNull(cause);
        assertTrue(cause.getMessage().contains("markerCommand"), "should name the bean: " + cause.getMessage());
        assertTrue(cause.getMessage().contains("ContainerCommand"), "should name the required type: " + cause.getMessage());
    }

    @Test
    void sharedSingletonContainerReusedForTwoBodiesFails() {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        // Deliberately a SINGLETON referenced twice - setBody would silently overwrite.
        factory.registerBeanDefinition("sharedRepeat",
                BeanDefinitionBuilder.genericBeanDefinition(RepeatCommand.class)
                        .addConstructorArgValue(2).getBeanDefinition());

        new XmlBeanDefinitionReader(factory).loadBeanDefinitions(flowXml("""
                <r:ruleflow name="SharedContainerFlow">
                    <r:command ref="sharedRepeat">
                        <r:exit/>
                    </r:command>
                    <r:command ref="sharedRepeat">
                        <r:exit/>
                    </r:command>
                </r:ruleflow>
                """));

        BeanCreationException ex = assertThrows(BeanCreationException.class,
                () -> factory.getBean("SharedContainerFlow"));

        UnrulyException cause = findCause(ex);
        assertNotNull(cause);
        assertTrue(cause.getMessage().contains("prototype"),
                "should point at the @Scope(\"prototype\") remedy: " + cause.getMessage());
    }

    private static ByteArrayResource flowXml(String body) {
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
                """ + body + "</beans>\n";
        return new ByteArrayResource(xml.getBytes(StandardCharsets.UTF_8), "container-test");
    }

    private static UnrulyException findCause(Throwable t) {
        while (t != null) {
            if (t instanceof UnrulyException ue) return ue;
            t = t.getCause();
        }
        return null;
    }
}
