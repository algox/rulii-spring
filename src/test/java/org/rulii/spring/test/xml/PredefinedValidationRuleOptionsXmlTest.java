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
import org.rulii.rule.RuleResult;
import org.rulii.ruleset.RuleSet;
import org.rulii.validation.RuleViolations;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ByteArrayResource;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The validator elements added for rulii 2.2's validators, and the settings of every validator's
 * builder as optional XML attributes.
 *
 * <p>Each setting is shown to reach the validator: a value whose outcome differs between the
 * element without the attribute (the builder's default) and the element with it. The data is the
 * data rulii's own validator tests use.
 *
 * @author Max Arulananthan
 * @since 2.3
 */
class PredefinedValidationRuleOptionsXmlTest {

    private static final String PASS = "PASS";
    private static final String FAIL = "FAIL";
    private static final String SKIPPED = "SKIPPED";

    private static final String UUID_V1 = "123e4567-e89b-12d3-a456-426614174000";
    private static final String UUID_V4 = "123e4567-e89b-42d3-a456-426614174000";
    private static final String UUID_NIL = "00000000-0000-0000-0000-000000000000";

    private static final Map<String, Object> PROPERTIES = Map.of(
            "luhn.start", "2",
            "site.host", "www.google.com",
            "uuid.versions", "4",
            "bad.port", "abc");

    // ------------------------------------------------------------------
    // Settings every validator shares
    // ------------------------------------------------------------------

    @Test
    void failOnNullSkipsTheRuleForANullValue() {
        assertEquals(FAIL, outcome("size", "min=\"1\" max=\"3\"", null));
        assertEquals(FAIL, outcome("size", "min=\"1\" max=\"3\" failOnNull=\"true\"", null));
        assertEquals(SKIPPED, outcome("size", "min=\"1\" max=\"3\" failOnNull=\"false\"", null));
        assertEquals(SKIPPED, outcome("isbn", "failOnNull=\"false\"", null));
        // Non-null values are validated as before.
        assertEquals(FAIL, outcome("size", "min=\"1\" max=\"3\" failOnNull=\"false\"", List.of()));
    }

    @Test
    void presenceRulesAcceptFailOnNullTrueOnly() {
        assertEquals(FAIL, outcome("notNull", "failOnNull=\"true\"", null));

        for (String type : List.of("notNull", "notBlank", "notEmpty")) {
            Exception failure = assertThrows(Exception.class, () -> rule(type, "failOnNull=\"false\""));
            assertTrue(messages(failure).contains("for attribute 'failOnNull'"), messages(failure));
        }
    }

    @Test
    void valueNameNamesTheValueInTheViolation() {
        assertTrue(violationParameters("min", "min=\"5\"", 1L).containsKey("value"));
        assertTrue(violationParameters("min", "min=\"5\" valueName=\"amount\"", 1L).containsKey("amount"));
    }

    // ------------------------------------------------------------------
    // Settings of the validators that already had an element
    // ------------------------------------------------------------------

    @Test
    void allowSpace() {
        flips("alpha", "", "allowSpace=\"true\"", "abc def", FAIL, PASS);
        flips("alphaNumeric", "", "allowSpace=\"true\"", "abc 123", FAIL, PASS);
        flips("numeric", "", "allowSpace=\"true\"", "123 45", FAIL, PASS);
        // Without the attribute the padded value passes on rulii 2.2.0 (the builder started with the
        // setting on) and fails from rulii 2.3.0; assert the half that holds on both until this
        // module builds against 2.3.0, then make it flips(..., FAIL, PASS) like the others.
        assertEquals(PASS, outcome("decimal", "allowSpace=\"true\"", "    100.00  "));
        // false is the default: nothing is switched on.
        assertEquals(FAIL, outcome("alpha", "allowSpace=\"false\"", "abc def"));
    }

    @Test
    void decimalBoundsInclusive() {
        flips("decimalMin", "min=\"10\"", "inclusive=\"false\"", new BigDecimal("10"), PASS, FAIL);
        flips("decimalMax", "max=\"10\"", "inclusive=\"false\"", new BigDecimal("10"), PASS, FAIL);
        assertEquals(PASS, outcome("decimalMin", "min=\"10\" inclusive=\"true\"", new BigDecimal("10")));
    }

    @Test
    void emailSettings() {
        flips("email", "", "allowLocal=\"true\"", "user@localhost", FAIL, PASS);
        flips("email", "", "allowTopLevelDomain=\"true\"", "bob@com", FAIL, PASS);
        flips("email", "", "anyDomain=\"true\"", "bob@intranet", FAIL, PASS);
        flips("email", "", "pattern=\"[a-z]+@acme\\.com\"", "bob@other.com", PASS, FAIL);
        flips("email", "pattern=\"[a-z]+@acme\\.com\"", "flags=\"CASE_INSENSITIVE\"", "Bob@acme.com", FAIL, PASS);
    }

    @Test
    void urlSettings() {
        flips("url", "", "protocol=\"https\"", "http://www.google.com", PASS, FAIL);
        flips("url", "", "host=\"www.google.com\"", "http://www.apple.com", PASS, FAIL);
        flips("url", "", "port=\"8080\"", "http://www.google.com/x", PASS, FAIL);
        assertEquals(PASS, outcome("url", "port=\"8080\"", "http://www.google.com:8080/x"));
        flips("url", "", "regexp=\".*apple.*\"", "http://www.google.com", PASS, FAIL);
        flips("url", "regexp=\".*GOOGLE.*\"", "flags=\"CASE_INSENSITIVE\"", "http://www.google.com", FAIL, PASS);
    }

    @Test
    void patternFlags() {
        flips("pattern", "pattern=\"a.b\"", "flags=\"DOTALL\"", "a\nb", FAIL, PASS);
        assertEquals(PASS, outcome("pattern", "pattern=\"a.b\" flags=\"DOTALL, CASE_INSENSITIVE\"", "A\nB"));
        // flags combine with caseSensitive, as they do in Java.
        assertEquals(PASS, outcome("pattern", "pattern=\"a.b\" caseSensitive=\"false\" flags=\"DOTALL\"", "A\nB"));
        assertEquals(FAIL, outcome("pattern", "pattern=\"a.b\" flags=\"DOTALL\"", "A\nB"));
    }

    // ------------------------------------------------------------------
    // The validators added in rulii 2.2
    // ------------------------------------------------------------------

    @Test
    void uniqueElements() {
        assertEquals(PASS, outcome("uniqueElements", "", List.of("a", "b")));
        assertEquals(FAIL, outcome("uniqueElements", "", List.of("a", "b", "a")));
    }

    @Test
    void normalized() {
        assertEquals(PASS, outcome("normalized", "", "café"));
        flips("normalized", "", "form=\"NFD\"", "café", FAIL, PASS);
    }

    @Test
    void codePointLength() {
        assertEquals(PASS, outcome("codePointLength", "min=\"2\" max=\"4\"", "😀😀"));
        assertEquals(FAIL, outcome("codePointLength", "min=\"2\" max=\"4\"", "a"));
        flips("codePointLength", "min=\"1\" max=\"1\"", "normalization=\"NFC\"", "é", FAIL, PASS);
    }

    @Test
    void uuid() {
        assertEquals(PASS, outcome("uuid", "", UUID_V1));
        assertEquals(FAIL, outcome("uuid", "", "123e4567-e89b-12d3-a456-42661417400"));
        flips("uuid", "", "allowEmpty=\"true\"", "", FAIL, PASS);
        flips("uuid", "", "allowNil=\"false\"", UUID_NIL, PASS, FAIL);
        flips("uuid", "", "versions=\"4\"", UUID_V1, PASS, FAIL);
        assertEquals(PASS, outcome("uuid", "versions=\"4\"", UUID_V4));
        assertEquals(PASS, outcome("uuid", "versions=\"1, 4\"", UUID_V1));
        flips("uuid", "", "variants=\"1\"", "123e4567-e89b-12d3-7456-426614174000", PASS, FAIL);
        flips("uuid", "", "letterCase=\"UPPER_CASE\"", "123E4567-E89B-12D3-A456-426614174000", FAIL, PASS);
        flips("uuid", "", "letterCase=\"INSENSITIVE\"", "123E4567-e89b-12d3-A456-426614174000", FAIL, PASS);
    }

    @Test
    void durationBounds() {
        assertEquals(PASS, outcome("durationMin", "min=\"PT5M\"", Duration.ofHours(1)));
        assertEquals(FAIL, outcome("durationMin", "min=\"PT5M\"", Duration.ofMinutes(4)));
        flips("durationMin", "min=\"PT5M\"", "inclusive=\"false\"", Duration.ofMinutes(5), PASS, FAIL);

        assertEquals(PASS, outcome("durationMax", "max=\"PT5M\"", Duration.ZERO));
        assertEquals(FAIL, outcome("durationMax", "max=\"PT5M\"", Duration.ofMinutes(6)));
        flips("durationMax", "max=\"PT5M\"", "inclusive=\"false\"", Duration.ofMinutes(5), PASS, FAIL);
    }

    @Test
    void luhnCheck() {
        assertEquals(PASS, outcome("luhnCheck", "", "79927398713"));
        assertEquals(FAIL, outcome("luhnCheck", "", "79927398710"));
        flips("luhnCheck", "", "startIndex=\"2\"", "5579927398713", FAIL, PASS);
        flips("luhnCheck", "", "endIndex=\"11\"", "79927398713yy", FAIL, PASS);
        flips("luhnCheck", "", "checkDigitIndex=\"0\"", "37992739871", FAIL, PASS);
        flips("luhnCheck", "", "ignoreNonDigitCharacters=\"false\"", "4111 1111 1111 1111", PASS, FAIL);
    }

    @Test
    void mod10Check() {
        assertEquals(PASS, outcome("mod10Check", "", "4006381333931"));
        assertEquals(FAIL, outcome("mod10Check", "", "4006381333932"));
        flips("mod10Check", "", "multiplier=\"1\"", "4006381333931", PASS, FAIL);
        flips("mod10Check", "", "weight=\"3\"", "1234567890128", PASS, FAIL);
        flips("mod10Check", "", "startIndex=\"2\"", "114006381333931", FAIL, PASS);
        flips("mod10Check", "", "endIndex=\"13\"", "4006381333931yy", FAIL, PASS);
        flips("mod10Check", "", "checkDigitIndex=\"0\"", "1400638133393", FAIL, PASS);
        flips("mod10Check", "", "ignoreNonDigitCharacters=\"false\"", "978-0-306-40615-7", PASS, FAIL);
    }

    @Test
    void mod11Check() {
        assertEquals(PASS, outcome("mod11Check", "", "0306406152"));
        assertEquals(FAIL, outcome("mod11Check", "", "0306406153"));
        flips("mod11Check", "", "threshold=\"3\"", "123458", FAIL, PASS);
        flips("mod11Check", "", "startIndex=\"2\"", "xx0306406152", FAIL, PASS);
        flips("mod11Check", "", "endIndex=\"10\"", "0306406152yy", FAIL, PASS);
        flips("mod11Check", "", "checkDigitIndex=\"0\"", "2030640615", FAIL, PASS);
        flips("mod11Check", "", "ignoreNonDigitCharacters=\"true\"", "0-306-40615-2", FAIL, PASS);
        flips("mod11Check", "", "treatCheck10As=\"A\"", "080442957A", FAIL, PASS);
        flips("mod11Check", "", "treatCheck11As=\"Z\"", "0Z", FAIL, PASS);
        flips("mod11Check", "", "processingDirection=\"LEFT_TO_RIGHT\"", "123457", FAIL, PASS);
    }

    @Test
    void ean() {
        assertEquals(PASS, outcome("ean", "", "4006381333931"));
        assertEquals(FAIL, outcome("ean", "", "4006381333932"));
        flips("ean", "", "type=\"EAN8\"", "73513537", FAIL, PASS);
        assertEquals(PASS, outcome("ean", "type=\"EAN13\"", "4006381333931"));
    }

    @Test
    void isbn() {
        assertEquals(PASS, outcome("isbn", "", "9780306406157"));
        assertEquals(FAIL, outcome("isbn", "", "9780306406158"));
        flips("isbn", "", "type=\"ISBN_10\"", "0306406152", FAIL, PASS);
        flips("isbn", "", "type=\"ANY\"", "0306406152", FAIL, PASS);
        assertEquals(PASS, outcome("isbn", "type=\"ANY\"", "9780306406157"));
    }

    // ------------------------------------------------------------------
    // Properties, rule sets, bad values
    // ------------------------------------------------------------------

    @Test
    void propertyPlaceholdersResolveInSettings() {
        assertEquals(PASS, outcome("luhnCheck", "startIndex=\"${luhn.start}\"", "5579927398713"));
        assertEquals(FAIL, outcome("url", "host=\"${site.host}\"", "http://www.apple.com"));
        assertEquals(FAIL, outcome("uuid", "versions=\"${uuid.versions}\"", UUID_V1));
    }

    @Test
    void newElementsAndSettingsWorkInsideARuleSet() {
        try (GenericApplicationContext context = load("""
                <r:ruleset name="Codes">
                    <r:rules>
                        <r:isbn name="CodesIsbn" binding="value" type="ANY"/>
                        <r:alpha name="CodesAlpha" binding="label" allowSpace="true"/>
                    </r:rules>
                </r:ruleset>
                """)) {
            assertNotNull(context.getBean("Codes", RuleSet.class));
            assertEquals(PASS, outcome(context.getBean("CodesIsbn", Rule.class), "value", "0306406152"));
            assertEquals(PASS, outcome(context.getBean("CodesAlpha", Rule.class), "label", "abc def"));
        }
    }

    @Test
    void aBadValueFailsAtStartupNamingTheRuleAndTheAttribute() {
        Exception duration = assertThrows(Exception.class, () -> rule("durationMin", "min=\"five minutes\""));
        assertTrue(messages(duration).contains("Predefined validation rule 'R' (durationMin): invalid value 'five minutes' for attribute 'min'"),
                messages(duration));

        // A value the schema cannot check, because it arrives through a property.
        Exception port = assertThrows(Exception.class, () -> rule("url", "port=\"${bad.port}\""));
        assertTrue(messages(port).contains("invalid value 'abc' for attribute 'port'"), messages(port));
    }

    @Test
    void theSchemaRejectsValuesOutsideASettingsRange() {
        assertThrows(Exception.class, () -> rule("pattern", "pattern=\"a\" flags=\"NO_SUCH_FLAG\""));
        assertThrows(Exception.class, () -> rule("isbn", "type=\"ISBN_11\""));
        assertThrows(Exception.class, () -> rule("mod11Check", "treatCheck10As=\"XY\""));
        assertThrows(Exception.class, () -> rule("uuid", "versions=\"four\""));
        assertThrows(Exception.class, () -> rule("luhnCheck", "startIndex=\"two\""));
        assertThrows(Exception.class, () -> rule("alpha", "allowSpace=\"yes\""));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Asserts the outcome for the value without the setting, then with it. */
    private static void flips(String type, String attributes, String setting, Object value, String without, String with) {
        assertEquals(without, outcome(type, attributes, value), "<" + type + " " + attributes + "> without " + setting);
        assertEquals(with, outcome(type, attributes + " " + setting, value), "<" + type + " " + attributes + " " + setting + ">");
    }

    private static String outcome(String type, String attributes, Object value) {
        return outcome(rule(type, attributes), "value", value);
    }

    private static String outcome(Rule rule, String bindingName, Object value) {
        RuleResult result = rule.run(context(bindingName, value));
        return result.status().isPass() ? PASS : result.status().isFail() ? FAIL : result.status().isSkipped() ? SKIPPED : result.status().toString();
    }

    private static Map<String, String> violationParameters(String type, String attributes, Object value) {
        RuleContext context = context("value", value);
        rule(type, attributes).run(context);
        RuleViolations violations = context.getBindings().getValue("ruleViolations", RuleViolations.class);
        assertEquals(1, violations.size());
        return violations.getViolation(0).getParameters();
    }

    private static RuleContext context(String bindingName, Object value) {
        RuleContext context = RuleContext.builder().standard().build();
        context.getBindings().bind("ruleViolations", new RuleViolations());

        if (value == null) context.getBindings().bind(bindingName, String.class, null);
        else context.getBindings().bind(bindingName, value);

        return context;
    }

    /** Builds the rule declared by {@code <r:type name="R" binding="value" attributes/>}. */
    private static Rule rule(String type, String attributes) {
        try (GenericApplicationContext context = load("<r:" + type + " name=\"R\" binding=\"value\" " + attributes + "/>")) {
            return context.getBean("R", Rule.class);
        }
    }

    private static GenericApplicationContext load(String elements) {
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
                """ + elements + "\n</beans>\n";

        GenericApplicationContext context = new GenericApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("options-test", PROPERTIES));
        context.registerBean(PropertySourcesPlaceholderConfigurer.class);
        new XmlBeanDefinitionReader(context).loadBeanDefinitions(
                new ByteArrayResource(xml.getBytes(StandardCharsets.UTF_8), "options test"));
        context.refresh();
        return context;
    }

    /** Every message in the cause chain, for assertions on a wrapped startup failure. */
    private static String messages(Throwable failure) {
        StringBuilder result = new StringBuilder();

        for (Throwable t = failure; t != null; t = t.getCause()) {
            result.append(t.getMessage()).append('\n');
        }

        return result.toString();
    }
}
