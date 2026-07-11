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
import org.rulii.rule.Rule;
import org.rulii.spring.xml.PredefinedValidationRuleFactoryBean;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ByteArrayResource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Sync guard for the predefined validator element list. Generates an XML document
 * containing every entry of {@link PredefinedValidationRuleFactoryBean#TYPES}, loads it
 * with schema validation enabled, and builds every resulting rule bean.
 *
 * <p>This locks the three places that must agree: the namespace handler registrations
 * (driven by TYPES), the XSD element declarations (validation would reject an undeclared
 * element), and the factory bean's dispatch switch (an uncovered type throws
 * "Unknown predefined validator type" at bean creation). Adding validator #38 to TYPES
 * without the matching switch case or XSD element fails here.
 *
 * @author Max Arulananthan
 * @since 2.0
 */
class PredefinedValidatorTypesTest {

    @Test
    void everyDeclaredTypeParsesValidatesAndBuilds() {
        StringBuilder xml = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xmlns:r="http://www.rulii.org/schema/rulii"
                       xsi:schemaLocation="
                           http://www.springframework.org/schema/beans
                           https://www.springframework.org/schema/beans/spring-beans.xsd
                           http://www.rulii.org/schema/rulii
                           https://www.rulii.org/spring/rulii-spring.xsd">
                """);

        for (String type : PredefinedValidationRuleFactoryBean.TYPES) {
            xml.append("    ").append(elementFor(type)).append('\n');
        }
        xml.append("</beans>\n");

        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
        reader.loadBeanDefinitions(new ByteArrayResource(
                xml.toString().getBytes(StandardCharsets.UTF_8), "generated from PredefinedValidationRuleFactoryBean.TYPES"));

        for (String type : PredefinedValidationRuleFactoryBean.TYPES) {
            Rule rule = factory.getBean(beanName(type), Rule.class);
            assertNotNull(rule, "predefined validator <" + type + "> must build a Rule");
        }
    }

    @Test
    void typeListIsComplete() {
        assertEquals(37, PredefinedValidationRuleFactoryBean.TYPES.size(),
                "update this count (and the docs) deliberately when adding/removing a validator element");
    }

    private static String beanName(String type) {
        return "Typed_" + type + "Rule";
    }

    /** Renders a schema-valid element for the given type, stubbing its required extras. */
    private static String elementFor(String type) {
        String attrs = switch (type) {
            case "min" -> " min=\"1\"";
            case "max" -> " max=\"10\"";
            case "decimalMin" -> " min=\"1.5\"";
            case "decimalMax" -> " max=\"9.5\"";
            case "size" -> " min=\"1\" max=\"5\"";
            case "digits" -> " maxIntegerDigits=\"5\" maxFractionDigits=\"2\"";
            case "pattern" -> " pattern=\"[a-z]+\"";
            case "assertEquals", "assertNotEquals" -> " value=\"x\"";
            default -> "";
        };

        String children = switch (type) {
            case "startsWith" -> "<r:prefix>a</r:prefix>";
            case "endsWith" -> "<r:suffix>a</r:suffix>";
            case "in" -> "<r:item>a</r:item>";
            default -> "";
        };

        String open = "<r:" + type + " name=\"" + beanName(type) + "\" binding=\"value\"" + attrs;
        return children.isEmpty() ? open + "/>" : open + ">" + children + "</r:" + type + ">";
    }
}
