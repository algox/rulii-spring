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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies that an inline rule name collision inside {@code <r:ruleset>} fails fast at
 * parse time with the XML source location, instead of silently reusing the first bean
 * registered under that name.
 *
 * <p>Regression test: {@code registerAndRef} previously skipped registration when the
 * name already existed, so the second ruleset silently executed the FIRST ruleset's rule
 * (or an arbitrary unrelated bean that happened to own the name).
 *
 * @author Max Arulananthan
 * @since 2.0
 */
class DuplicateInlineRuleXmlTest {

    @Test
    void duplicateInlineRuleNameFailsAtParseTime() {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);

        BeanDefinitionParsingException ex = assertThrows(BeanDefinitionParsingException.class,
                () -> reader.loadBeanDefinitions(new ClassPathResource("rules/duplicate-inline-rule-test.xml")));

        assertTrue(ex.getMessage().contains("Duplicate rule bean name [DuplicateInlineRule]"),
                "error should name the duplicate rule, but was: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("<bean-ref name=\"DuplicateInlineRule\"/>"),
                "error should suggest the bean-ref remedy, but was: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("duplicate-inline-rule-test.xml"),
                "error should include the XML resource location, but was: " + ex.getMessage());
    }
}
