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
import org.rulii.validation.ValueValidationRuleBuilder;
import org.rulii.validation.rules.Validators;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Sync guard for the predefined validator element list. Generates an XML document
 * containing every entry of {@link PredefinedValidationRuleFactoryBean#TYPES}, loads it
 * with schema validation enabled, and builds every resulting rule bean.
 *
 * <p>This locks the three places that must agree: the namespace handler registrations
 * (driven by TYPES), the XSD element declarations (validation would reject an undeclared
 * element), and the factory bean's dispatch switch (an uncovered type throws
 * "Unknown predefined validator type" at bean creation). Adding a validator to TYPES
 * without the matching switch case or XSD element fails here.
 *
 * <p>It also locks the XML against rulii itself: every validator {@link Validators} builds has
 * an element, and every setting of a validator's builder is an optional attribute of that
 * element ({@link PredefinedValidationRuleFactoryBean#OPTIONS}, checked against the builders
 * and against the XSD). A validator or a setting added in rulii fails here until the XML has it.
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
        assertEquals(48, PredefinedValidationRuleFactoryBean.TYPES.size(),
                "update this count (and the docs) deliberately when adding/removing a validator element");
    }

    /** Every validator rulii builds through {@link Validators} has an XML element, and nothing else does. */
    @Test
    void everyValidatorHasAnElement() {
        assertEquals(new TreeSet<>(builders().keySet()), new TreeSet<>(PredefinedValidationRuleFactoryBean.TYPES),
                "a validator in org.rulii.validation.rules.Validators without an XML element (or the reverse): "
                        + "add it to TYPES, the factory bean's switch and the XSD");
    }

    /**
     * Every setting of the builder all validators share is accounted for: a common attribute of
     * every element, taken from the bean definition, or knowingly left out of the XML.
     */
    @Test
    void everySharedBuilderSettingIsAccountedFor() {
        Set<String> expected = new TreeSet<>(PredefinedValidationRuleFactoryBean.COMMON_OPTIONS);
        // The common override attributes (message is the errorMessage attribute).
        expected.addAll(Set.of("name", "description", "category", "tags", "errorCode", "severity", "message"));
        // Set by the factory bean from the bean definition's XML file and line.
        expected.add("source");
        // Not available in XML: name/value pairs have no attribute form.
        expected.addAll(Set.of("param", "params"));

        assertEquals(expected, sharedSettings(),
                "a setting of ValueValidationRuleBuilder that the XML does not account for (or the reverse)");
    }

    /** Every setting of a validator's own builder is in OPTIONS, and nothing else is. */
    @Test
    void everyBuilderSettingIsAnOption() {
        Map<String, Set<String>> expected = new TreeMap<>();
        Set<String> shared = sharedSettings();

        builders().forEach((type, builder) -> {
            Set<String> settings = new TreeSet<>();

            for (Method method : builder.getDeclaredMethods()) {
                if (Modifier.isPublic(method.getModifiers()) && !method.isSynthetic() && method.getReturnType() == builder) {
                    settings.add(method.getName());
                }
            }

            // A builder may override a shared setting (the presence rules restrict failOnNull); it stays a common option.
            settings.removeAll(shared);

            // caseSensitive predates OPTIONS and stays a property of its own on the factory bean.
            if (type.equals("pattern")) settings.remove("caseSensitive");
            if (!settings.isEmpty()) expected.put(type, settings);
        });

        assertEquals(expected, declaredOptions(),
                "a builder setting without an XML attribute (or the reverse): add it to OPTIONS, "
                        + "the factory bean's builder method and the XSD");
    }

    /** Every option is an optional attribute of its element in the XSD, and the XSD declares no others. */
    @Test
    void everyOptionIsAnOptionalXsdAttribute() throws Exception {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        Document xsd;

        try (InputStream in = PredefinedValidationRuleFactoryBean.class.getResourceAsStream("rulii-spring.xsd")) {
            xsd = documentBuilderFactory.newDocumentBuilder().parse(in);
        }

        Map<String, Set<String>> actual = new TreeMap<>();

        for (Element element : DomUtils.getChildElementsByTagName(xsd.getDocumentElement(), "element")) {
            String type = element.getAttribute("name");
            if (!PredefinedValidationRuleFactoryBean.TYPES.contains(type)) continue;

            Set<String> optional = new TreeSet<>();
            NodeList attributes = element.getElementsByTagNameNS("http://www.w3.org/2001/XMLSchema", "attribute");

            for (int i = 0; i < attributes.getLength(); i++) {
                Element attribute = (Element) attributes.item(i);
                if (!"required".equals(attribute.getAttribute("use"))) optional.add(attribute.getAttribute("name"));
                assertFalse(attribute.hasAttribute("default") && !"caseSensitive".equals(attribute.getAttribute("name")),
                        "<" + type + " " + attribute.getAttribute("name") + ">: an option declares no XSD default, "
                                + "so that leaving it out keeps the builder's default");
            }

            if (type.equals("pattern")) optional.remove("caseSensitive");
            if (!optional.isEmpty()) actual.put(type, optional);
        }

        assertEquals(declaredOptions(), actual);

        // The common options are optional attributes of the base type every element extends.
        Set<String> common = new TreeSet<>();

        for (Element complexType : DomUtils.getChildElementsByTagName(xsd.getDocumentElement(), "complexType")) {
            if (!"predefinedValidationRuleType".equals(complexType.getAttribute("name"))) continue;

            for (Element attribute : DomUtils.getChildElementsByTagName(complexType, "attribute")) {
                if (PredefinedValidationRuleFactoryBean.COMMON_OPTIONS.contains(attribute.getAttribute("name"))) {
                    assertFalse(attribute.hasAttribute("default") || attribute.hasAttribute("use"));
                    common.add(attribute.getAttribute("name"));
                }
            }
        }

        assertEquals(new TreeSet<>(PredefinedValidationRuleFactoryBean.COMMON_OPTIONS), common);
    }

    /** The settings of the builder every validator shares: its public methods that return the builder. */
    private static Set<String> sharedSettings() {
        Set<String> result = new TreeSet<>();

        for (Method method : ValueValidationRuleBuilder.class.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers()) && !method.isSynthetic()
                    && method.getReturnType() == ValueValidationRuleBuilder.class) {
                result.add(method.getName());
            }
        }

        return result;
    }

    private static Map<String, Set<String>> declaredOptions() {
        Map<String, Set<String>> result = new TreeMap<>();
        PredefinedValidationRuleFactoryBean.OPTIONS.forEach((type, options) -> result.put(type, new TreeSet<>(options)));
        return result;
    }

    /** The factory methods of {@link Validators} that build a validation rule, with the builder each returns. */
    private static Map<String, Class<?>> builders() {
        Map<String, Class<?>> result = new TreeMap<>();

        for (Method method : Validators.class.getMethods()) {
            if (Modifier.isStatic(method.getModifiers()) && ValueValidationRuleBuilder.class.isAssignableFrom(method.getReturnType())) {
                result.put(method.getName(), method.getReturnType());
            }
        }

        return result;
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
            case "size", "codePointLength" -> " min=\"1\" max=\"5\"";
            case "durationMin" -> " min=\"PT1M\"";
            case "durationMax" -> " max=\"PT1H\"";
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
