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
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that expression-bearing elements without an expression, and predefined
 * validators without a value source, fail fast at XML parse time with the element name
 * and source location.
 *
 * <p>Regression tests: these documents previously aborted with a context-free
 * {@code IllegalArgumentException: expression cannot be null.} (the friendly diagnostic
 * was unreachable dead code), and multi-value validators glued their child-element text
 * together and compiled it as a script expression.
 *
 * @author Max Arulananthan
 * @since 2.0
 */
class ExpressionErrorXmlTest {

    @Test
    void emptyGivenFailsAtParseTimeWithLocation() {
        BeanDefinitionParsingException ex = load("rules/expr-error/empty-given-test.xml");

        assertTrue(ex.getMessage().contains("<given> must provide a script expression"),
                "error should name the empty element, but was: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("empty-given-test.xml"),
                "error should include the XML resource location, but was: " + ex.getMessage());
    }

    @Test
    void missingValueSourceFailsAtParseTimeWithDiagnostic() {
        BeanDefinitionParsingException ex = load("rules/expr-error/missing-value-source-test.xml");

        assertTrue(ex.getMessage().contains("<notNull> must supply either a binding attribute or a script expression"),
                "error should carry the binding/expr diagnostic, but was: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("missing-value-source-test.xml"),
                "error should include the XML resource location, but was: " + ex.getMessage());
    }

    @Test
    void childItemTextIsNotTreatedAsValueSourceExpression() {
        BeanDefinitionParsingException ex = load("rules/expr-error/items-only-test.xml");

        assertTrue(ex.getMessage().contains("<in> must supply either a binding attribute or a script expression"),
                "child <item> text must not masquerade as the value-source expression, but was: " + ex.getMessage());
    }

    private static BeanDefinitionParsingException load(String location) {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
        return assertThrows(BeanDefinitionParsingException.class,
                () -> reader.loadBeanDefinitions(new ClassPathResource(location)));
    }
}
