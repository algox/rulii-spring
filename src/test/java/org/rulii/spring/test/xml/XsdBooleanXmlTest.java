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
import org.rulii.ruleset.RuleSet;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies xs:boolean attribute handling in the rulii namespace.
 *
 * <p>Regression tests: {@code Boolean.parseBoolean} treated the schema-valid literal
 * {@code "1"} as {@code false}, silently inverting {@code caseSensitive="1"} and
 * {@code required="1"}; and the {@code validating} attribute was passed to a primitive
 * boolean property as the raw string, which only converted because XSD validation
 * injects the schema default — loading with validation disabled crashed.
 *
 * @author Max Arulananthan
 * @since 2.0
 */
class XsdBooleanXmlTest {

    private DefaultListableBeanFactory load(String location, boolean validating) {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
        if (!validating) {
            // Switching validation off also requires enabling namespace awareness explicitly
            // (see XmlBeanDefinitionReader.setValidationMode javadoc).
            reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
            reader.setNamespaceAware(true);
        }
        reader.loadBeanDefinitions(new ClassPathResource(location));
        return factory;
    }

    @Test
    void caseSensitiveOneMeansCaseSensitive() {
        DefaultListableBeanFactory factory = load("rules/xsd-boolean-test.xml", true);
        Rule rule = factory.getBean("StrictCodeRule", Rule.class);

        RuleContext lower = RuleContext.builder().standard().build();
        lower.getBindings().bind("code", "abc");
        assertTrue(rule.isTrue(lower), "lowercase code matches [a-z]+ case-sensitively");

        RuleContext upper = RuleContext.builder().standard().build();
        upper.getBindings().bind("code", "ABC");
        assertFalse(rule.isTrue(upper), "caseSensitive=\"1\" must mean case-SENSITIVE: ABC must not match [a-z]+");
    }

    @Test
    void requiredOneMeansRequired() {
        DefaultListableBeanFactory factory = load("rules/xsd-boolean-test.xml", true);
        RuleSet<?> ruleSet = factory.getBean("RequiredParamRuleSet", RuleSet.class);

        RuleContext bound = RuleContext.builder().standard().build();
        bound.getBindings().bind("amount", 10);
        assertDoesNotThrow(() -> ruleSet.run(bound));

        RuleContext unbound = RuleContext.builder().standard().build();
        assertThrows(Exception.class, () -> ruleSet.run(unbound),
                "required=\"1\" must mean REQUIRED: running without the binding must fail");
    }

    @Test
    void rulesetParsesWithoutSchemaValidation() {
        // With validation off, no schema defaults are injected; the absent validating
        // attribute must not blow up the primitive boolean property conversion.
        DefaultListableBeanFactory factory = load("rules/no-validation-test.xml", false);
        assertNotNull(factory.getBean("NoValidationRuleSet", RuleSet.class));
    }
}
