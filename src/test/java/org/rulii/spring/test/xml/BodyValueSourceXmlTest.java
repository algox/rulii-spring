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
import org.rulii.rule.Rule;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the documented text-body value source form for predefined validators:
 * {@code <r:notNull name="...">#ctx.person</r:notNull>}.
 *
 * <p>Regression test: {@code predefinedValidationRuleType} was not declared
 * {@code mixed="true"}, so this form — documented in the XSD's own comments — was
 * rejected by schema validation (which is enabled by default and in this test).
 *
 * @author Max Arulananthan
 * @since 2.0
 */
class BodyValueSourceXmlTest {

    @Test
    void textBodyValueSourceValidatesAndEvaluates() {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
        reader.loadBeanDefinitions(new ClassPathResource("rules/body-value-source-test.xml"));

        Rule rule = factory.getBean("BodyNotNullRule", Rule.class);
        assertNotNull(rule);

        RuleContext boundCtx = RuleContext.builder().standard().build();
        boundCtx.getBindings().bind("person", "Max");
        assertTrue(rule.isTrue(boundCtx));

        RuleContext nullCtx = RuleContext.builder().standard().build();
        nullCtx.getBindings().bind("person", String.class, null);
        assertFalse(rule.isTrue(nullCtx));
    }
}
