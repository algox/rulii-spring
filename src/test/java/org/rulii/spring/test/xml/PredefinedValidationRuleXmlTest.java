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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.rulii.context.RuleContext;
import org.rulii.rule.Rule;
import org.rulii.rule.RuleResult;
import org.rulii.ruleset.RuleSet;
import org.rulii.validation.RuleViolations;
import org.rulii.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the predefined validation rule XML namespace elements.
 *
 * <p>Covers:
 * <ul>
 *   <li>No-param rules: notNull, notBlank, isNull, blank, email, url, lowerCase, upperCase,
 *       alpha, numeric, assertTrue, assertFalse, positive, positiveOrZero, negative, negativeOrZero</li>
 *   <li>Numeric bound rules: min, max, decimalMin, decimalMax</li>
 *   <li>Size / digit rules: size, digits</li>
 *   <li>Pattern rules: case-sensitive and case-insensitive</li>
 *   <li>Equality rules: assertEquals, assertNotEquals</li>
 *   <li>Multi-value rules: in, startsWith, endsWith</li>
 *   <li>Script expression form (expr attribute instead of binding)</li>
 *   <li>Override form (errorCode, severity, errorMessage)</li>
 *   <li>Ruleset integration: predefined rules embedded in a validating ruleset</li>
 * </ul>
 *
 * @author Max Arulananthan
 * @since 1.0
 */
@SpringJUnitConfig(locations = "classpath:rules/predefined-validation-rule-test.xml")
class PredefinedValidationRuleXmlTest {

    // No-param: null / blank
    @Autowired @Qualifier("NotNullRule")
    private Rule notNullRule;

    @Autowired @Qualifier("NotBlankRule")
    private Rule notBlankRule;

    @Autowired @Qualifier("IsNullRule")
    private Rule isNullRule;

    @Autowired @Qualifier("BlankRule")
    private Rule blankRule;

    // No-param: string format
    @Autowired @Qualifier("EmailRule")
    private Rule emailRule;

    @Autowired @Qualifier("UrlRule")
    private Rule urlRule;

    @Autowired @Qualifier("LowerCaseRule")
    private Rule lowerCaseRule;

    @Autowired @Qualifier("UpperCaseRule")
    private Rule upperCaseRule;

    @Autowired @Qualifier("AlphaRule")
    private Rule alphaRule;

    @Autowired @Qualifier("NumericRule")
    private Rule numericRule;

    // No-param: booleans
    @Autowired @Qualifier("AssertTrueRule")
    private Rule assertTrueRule;

    @Autowired @Qualifier("AssertFalseRule")
    private Rule assertFalseRule;

    // No-param: sign
    @Autowired @Qualifier("PositiveRule")
    private Rule positiveRule;

    @Autowired @Qualifier("PositiveOrZeroRule")
    private Rule positiveOrZeroRule;

    @Autowired @Qualifier("NegativeRule")
    private Rule negativeRule;

    @Autowired @Qualifier("NegativeOrZeroRule")
    private Rule negativeOrZeroRule;

    // Numeric bounds
    @Autowired @Qualifier("MinAgeRule")
    private Rule minAgeRule;

    @Autowired @Qualifier("MaxAgeRule")
    private Rule maxAgeRule;

    @Autowired @Qualifier("DecimalMinScoreRule")
    private Rule decimalMinScoreRule;

    @Autowired @Qualifier("DecimalMaxScoreRule")
    private Rule decimalMaxScoreRule;

    // Size / digits
    @Autowired @Qualifier("NameSizeRule")
    private Rule nameSizeRule;

    @Autowired @Qualifier("PriceDigitsRule")
    private Rule priceDigitsRule;

    // Pattern
    @Autowired @Qualifier("CodePatternRule")
    private Rule codePatternRule;

    @Autowired @Qualifier("CaseInsensitivePatternRule")
    private Rule caseInsensitivePatternRule;

    // Equality
    @Autowired @Qualifier("StatusEqualsActiveRule")
    private Rule statusEqualsActiveRule;

    @Autowired @Qualifier("StatusNotDeletedRule")
    private Rule statusNotDeletedRule;

    // Multi-value
    @Autowired @Qualifier("StatusInRule")
    private Rule statusInRule;

    @Autowired @Qualifier("TitleStartsWithRule")
    private Rule titleStartsWithRule;

    @Autowired @Qualifier("EmailEndsWithRule")
    private Rule emailEndsWithRule;

    // Expr form
    @Autowired @Qualifier("MinAgeByExprRule")
    private Rule minAgeByExprRule;

    // Override form
    @Autowired @Qualifier("OverriddenNotNullRule")
    private Rule overriddenNotNullRule;

    // Ruleset integration
    @Autowired @Qualifier("PersonValidationRuleSet")
    private RuleSet<?> personValidationRuleSet;

    // ------------------------------------------------------------------
    // notNull
    // ------------------------------------------------------------------

    @Nested
    class NotNullRuleTests {

        @Test
        void beanIsCreated() {
            assertNotNull(notNullRule);
        }

        @Test
        void passesWhenValueIsNonNull() {
            RuleContext ctx = ctx();
            ctx.getBindings().bind("value", "present");
            assertTrue(notNullRule.isTrue(ctx));
        }

        @Test
        void failsWhenValueIsNull() {
            RuleContext ctx = ctx();
            ctx.getBindings().bind("value", (String) null);
            assertFalse(notNullRule.isTrue(ctx));
        }

        @Test
        void runRecordsViolationWhenValueIsNull() {
            RuleContext ctx = ctxWithViolations();
            ctx.getBindings().bind("value", (String) null);
            RuleResult result = notNullRule.run(ctx);
            assertTrue(result.status().isFail());
            assertEquals(1, violations(ctx).size());
        }

        @Test
        void runPassesWithoutViolationWhenValuePresent() {
            RuleContext ctx = ctxWithViolations();
            ctx.getBindings().bind("value", 42);
            RuleResult result = notNullRule.run(ctx);
            assertTrue(result.status().isPass());
            assertTrue(violations(ctx).isEmpty());
        }
    }


    // ------------------------------------------------------------------
    // notBlank
    // ------------------------------------------------------------------

    @Nested
    class NotBlankRuleTests {

        @Test
        void beanIsCreated() {
            assertNotNull(notBlankRule);
        }

        @Test
        void passesForNonBlankText() {
            assertTrue(notBlankRule.isTrue(ctx("text", "hello")));
        }

        @Test
        void failsForEmptyString() {
            assertFalse(notBlankRule.isTrue(ctx("text", "")));
        }

        @Test
        void failsForBlankString() {
            assertFalse(notBlankRule.isTrue(ctx("text", "   ")));
        }

        @Test
        void runRecordsViolationForBlankText() {
            RuleContext ctx = ctxWithViolations("text", "  ");
            notBlankRule.run(ctx);
            assertEquals(1, violations(ctx).size());
        }
    }


    // ------------------------------------------------------------------
    // isNull
    // ------------------------------------------------------------------

    @Nested
    class IsNullRuleTests {

        @Test
        void beanIsCreated() {
            assertNotNull(isNullRule);
        }

        @Test
        void passesWhenValueIsNull() {
            RuleContext ctx = ctx();
            ctx.getBindings().bind("value", (String) null);
            assertTrue(isNullRule.isTrue(ctx));
        }

        @Test
        void failsWhenValueIsNonNull() {
            // Explicit non-null value should fail the condition
            RuleContext ctx = ctx();
            ctx.getBindings().bind("value", "something");
            assertFalse(isNullRule.isTrue(ctx));
        }
    }


    // ------------------------------------------------------------------
    // blank
    // ------------------------------------------------------------------

    @Nested
    class BlankRuleTests {

        @Test
        void beanIsCreated() {
            assertNotNull(blankRule);
        }

        @Test
        void passesForEmptyString() {
            assertTrue(blankRule.isTrue(ctx("text", "")));
        }

        @Test
        void passesForWhitespaceOnly() {
            assertTrue(blankRule.isTrue(ctx("text", "   ")));
        }

        @Test
        void failsForNonBlankText() {
            assertFalse(blankRule.isTrue(ctx("text", "hello")));
        }
    }


    // ------------------------------------------------------------------
    // email
    // ------------------------------------------------------------------

    @Nested
    class EmailRuleTests {

        @Test
        void beanIsCreated() {
            assertNotNull(emailRule);
        }

        @Test
        void passesForValidEmail() {
            assertTrue(emailRule.isTrue(ctx("email", "user@example.com")));
        }

        @Test
        void failsForInvalidEmail() {
            assertFalse(emailRule.isTrue(ctx("email", "not-an-email")));
        }

        @Test
        void failsForMissingDomain() {
            assertFalse(emailRule.isTrue(ctx("email", "user@")));
        }

        @Test
        void runRecordsViolationForInvalidEmail() {
            RuleContext ctx = ctxWithViolations("email", "bad-email");
            emailRule.run(ctx);
            assertEquals(1, violations(ctx).size());
        }
    }


    // ------------------------------------------------------------------
    // url
    // ------------------------------------------------------------------

    @Nested
    class UrlRuleTests {

        @Test
        void beanIsCreated() {
            assertNotNull(urlRule);
        }

        @Test
        void passesForValidHttpUrl() {
            assertTrue(urlRule.isTrue(ctx("url", "http://example.com")));
        }

        @Test
        void passesForValidHttpsUrl() {
            assertTrue(urlRule.isTrue(ctx("url", "https://www.example.org/path?q=1")));
        }

        @Test
        void failsForInvalidUrl() {
            assertFalse(urlRule.isTrue(ctx("url", "not a url")));
        }
    }


    // ------------------------------------------------------------------
    // lowerCase / upperCase
    // ------------------------------------------------------------------

    @Nested
    class CaseRuleTests {

        @Test
        void lowerCaseRuleBeanIsCreated() {
            assertNotNull(lowerCaseRule);
        }

        @Test
        void lowerCasePassesForAllLowercase() {
            assertTrue(lowerCaseRule.isTrue(ctx("text", "hello")));
        }

        @Test
        void lowerCaseFailsForUppercase() {
            assertFalse(lowerCaseRule.isTrue(ctx("text", "HELLO")));
        }

        @Test
        void lowerCaseFailsForMixedCase() {
            assertFalse(lowerCaseRule.isTrue(ctx("text", "Hello")));
        }

        @Test
        void upperCaseRuleBeanIsCreated() {
            assertNotNull(upperCaseRule);
        }

        @Test
        void upperCasePassesForAllUppercase() {
            assertTrue(upperCaseRule.isTrue(ctx("text", "HELLO")));
        }

        @Test
        void upperCaseFailsForLowercase() {
            assertFalse(upperCaseRule.isTrue(ctx("text", "hello")));
        }
    }


    // ------------------------------------------------------------------
    // alpha / numeric
    // ------------------------------------------------------------------

    @Nested
    class AlphaNumericRuleTests {

        @Test
        void alphaRuleBeanIsCreated() {
            assertNotNull(alphaRule);
        }

        @Test
        void alphaPassesForLettersOnly() {
            assertTrue(alphaRule.isTrue(ctx("text", "hello")));
        }

        @Test
        void alphaFailsForStringWithDigits() {
            assertFalse(alphaRule.isTrue(ctx("text", "hello123")));
        }

        @Test
        void numericRuleBeanIsCreated() {
            assertNotNull(numericRule);
        }

        @Test
        void numericPassesForDigitsOnly() {
            assertTrue(numericRule.isTrue(ctx("text", "12345")));
        }

        @Test
        void numericFailsForStringWithLetters() {
            assertFalse(numericRule.isTrue(ctx("text", "123abc")));
        }
    }


    // ------------------------------------------------------------------
    // assertTrue / assertFalse
    // ------------------------------------------------------------------

    @Nested
    class AssertBooleanRuleTests {

        @Test
        void assertTrueRuleBeanIsCreated() {
            assertNotNull(assertTrueRule);
        }

        @Test
        void assertTruePassesWhenFlagIsTrue() {
            assertTrue(assertTrueRule.isTrue(ctx("flag", Boolean.TRUE)));
        }

        @Test
        void assertTrueFailsWhenFlagIsFalse() {
            assertFalse(assertTrueRule.isTrue(ctx("flag", Boolean.FALSE)));
        }

        @Test
        void assertFalseRuleBeanIsCreated() {
            assertNotNull(assertFalseRule);
        }

        @Test
        void assertFalsePassesWhenFlagIsFalse() {
            assertTrue(assertFalseRule.isTrue(ctx("flag", Boolean.FALSE)));
        }

        @Test
        void assertFalseFailsWhenFlagIsTrue() {
            assertFalse(assertFalseRule.isTrue(ctx("flag", Boolean.TRUE)));
        }
    }


    // ------------------------------------------------------------------
    // positive / positiveOrZero / negative / negativeOrZero
    // ------------------------------------------------------------------

    @Nested
    class SignRuleTests {

        @Test
        void positiveRuleBeanIsCreated() {
            assertNotNull(positiveRule);
        }

        @Test
        void positivePassesForPositiveNumber() {
            assertTrue(positiveRule.isTrue(ctx("number", 1)));
        }

        @Test
        void positiveFailsForZero() {
            assertFalse(positiveRule.isTrue(ctx("number", 0)));
        }

        @Test
        void positiveFailsForNegativeNumber() {
            assertFalse(positiveRule.isTrue(ctx("number", -1)));
        }

        @Test
        void positiveOrZeroRuleBeanIsCreated() {
            assertNotNull(positiveOrZeroRule);
        }

        @Test
        void positiveOrZeroPassesForZero() {
            assertTrue(positiveOrZeroRule.isTrue(ctx("number", 0)));
        }

        @Test
        void positiveOrZeroPassesForPositiveNumber() {
            assertTrue(positiveOrZeroRule.isTrue(ctx("number", 5)));
        }

        @Test
        void positiveOrZeroFailsForNegativeNumber() {
            assertFalse(positiveOrZeroRule.isTrue(ctx("number", -1)));
        }

        @Test
        void negativeRuleBeanIsCreated() {
            assertNotNull(negativeRule);
        }

        @Test
        void negativePassesForNegativeNumber() {
            assertTrue(negativeRule.isTrue(ctx("number", -1)));
        }

        @Test
        void negativeFailsForZero() {
            assertFalse(negativeRule.isTrue(ctx("number", 0)));
        }

        @Test
        void negativeFailsForPositiveNumber() {
            assertFalse(negativeRule.isTrue(ctx("number", 1)));
        }

        @Test
        void negativeOrZeroRuleBeanIsCreated() {
            assertNotNull(negativeOrZeroRule);
        }

        @Test
        void negativeOrZeroPassesForZero() {
            assertTrue(negativeOrZeroRule.isTrue(ctx("number", 0)));
        }

        @Test
        void negativeOrZeroPassesForNegativeNumber() {
            assertTrue(negativeOrZeroRule.isTrue(ctx("number", -5)));
        }

        @Test
        void negativeOrZeroFailsForPositiveNumber() {
            assertFalse(negativeOrZeroRule.isTrue(ctx("number", 1)));
        }
    }


    // ------------------------------------------------------------------
    // Decimal string coercion (rulii 2.0: string values coerce via
    // BigDecimal — decimal strings no longer falsely FAIL)
    // ------------------------------------------------------------------

    @Nested
    class DecimalStringCoercionTests {

        @Test
        void minAgePassesForDecimalStringAboveBoundary() {
            assertTrue(minAgeRule.isTrue(ctx("age", "18.5")));
        }

        @Test
        void minAgeFailsForDecimalStringBelowBoundary() {
            assertFalse(minAgeRule.isTrue(ctx("age", "17.5")));
        }

        @Test
        void maxAgeFailsForDecimalStringAboveBoundary() {
            assertFalse(maxAgeRule.isTrue(ctx("age", "100.5")));
        }

        @Test
        void positivePassesForPositiveDecimalString() {
            assertTrue(positiveRule.isTrue(ctx("number", "0.5")));
        }

        @Test
        void negativeFailsForPositiveDecimalString() {
            assertFalse(negativeRule.isTrue(ctx("number", "0.5")));
        }
    }


    // ------------------------------------------------------------------
    // min / max (integer bounds)
    // ------------------------------------------------------------------

    @Nested
    class IntegerBoundsRuleTests {

        @Test
        void minAgeRuleBeanIsCreated() {
            assertNotNull(minAgeRule);
        }

        @Test
        void minAgePassesAtExactBoundary() {
            assertTrue(minAgeRule.isTrue(ctx("age", 18)));
        }

        @Test
        void minAgePassesAboveBoundary() {
            assertTrue(minAgeRule.isTrue(ctx("age", 25)));
        }

        @Test
        void minAgeFailsBelowBoundary() {
            assertFalse(minAgeRule.isTrue(ctx("age", 17)));
        }

        @Test
        void minAgeRunRecordsViolationBelowBoundary() {
            RuleContext ctx = ctxWithViolations("age", 10);
            minAgeRule.run(ctx);
            assertEquals(1, violations(ctx).size());
        }

        @Test
        void maxAgeRuleBeanIsCreated() {
            assertNotNull(maxAgeRule);
        }

        @Test
        void maxAgePassesAtExactBoundary() {
            assertTrue(maxAgeRule.isTrue(ctx("age", 100)));
        }

        @Test
        void maxAgePassesBelowBoundary() {
            assertTrue(maxAgeRule.isTrue(ctx("age", 50)));
        }

        @Test
        void maxAgeFailsAboveBoundary() {
            assertFalse(maxAgeRule.isTrue(ctx("age", 101)));
        }

        @Test
        void maxAgeRunRecordsViolationAboveBoundary() {
            RuleContext ctx = ctxWithViolations("age", 150);
            maxAgeRule.run(ctx);
            assertEquals(1, violations(ctx).size());
        }
    }


    // ------------------------------------------------------------------
    // decimalMin / decimalMax
    // ------------------------------------------------------------------

    @Nested
    class DecimalBoundsRuleTests {

        @Test
        void decimalMinRuleBeanIsCreated() {
            assertNotNull(decimalMinScoreRule);
        }

        @Test
        void decimalMinPassesAtExactBoundary() {
            assertTrue(decimalMinScoreRule.isTrue(ctx("score", new BigDecimal("0.0"))));
        }

        @Test
        void decimalMinPassesAboveBoundary() {
            assertTrue(decimalMinScoreRule.isTrue(ctx("score", new BigDecimal("5.5"))));
        }

        @Test
        void decimalMinFailsBelowBoundary() {
            assertFalse(decimalMinScoreRule.isTrue(ctx("score", new BigDecimal("-0.1"))));
        }

        @Test
        void decimalMinRunRecordsViolation() {
            RuleContext ctx = ctxWithViolations("score", new BigDecimal("-1.0"));
            decimalMinScoreRule.run(ctx);
            assertEquals(1, violations(ctx).size());
        }

        @Test
        void decimalMaxRuleBeanIsCreated() {
            assertNotNull(decimalMaxScoreRule);
        }

        @Test
        void decimalMaxPassesAtExactBoundary() {
            assertTrue(decimalMaxScoreRule.isTrue(ctx("score", new BigDecimal("10.0"))));
        }

        @Test
        void decimalMaxPassesBelowBoundary() {
            assertTrue(decimalMaxScoreRule.isTrue(ctx("score", new BigDecimal("7.5"))));
        }

        @Test
        void decimalMaxFailsAboveBoundary() {
            assertFalse(decimalMaxScoreRule.isTrue(ctx("score", new BigDecimal("10.1"))));
        }

        @Test
        void decimalMaxRunRecordsViolation() {
            RuleContext ctx = ctxWithViolations("score", new BigDecimal("999.0"));
            decimalMaxScoreRule.run(ctx);
            assertEquals(1, violations(ctx).size());
        }
    }


    // ------------------------------------------------------------------
    // size
    // ------------------------------------------------------------------

    @Nested
    class SizeRuleTests {

        @Test
        void beanIsCreated() {
            assertNotNull(nameSizeRule);
        }

        @Test
        void passesForNameWithinBounds() {
            assertTrue(nameSizeRule.isTrue(ctx("name", "Alice")));
        }

        @Test
        void passesForNameAtMinBoundary() {
            assertTrue(nameSizeRule.isTrue(ctx("name", "Al")));
        }

        @Test
        void passesForNameAtMaxBoundary() {
            assertTrue(nameSizeRule.isTrue(ctx("name", "A".repeat(50))));
        }

        @Test
        void failsForNameBelowMin() {
            assertFalse(nameSizeRule.isTrue(ctx("name", "A")));
        }

        @Test
        void failsForNameAboveMax() {
            assertFalse(nameSizeRule.isTrue(ctx("name", "A".repeat(51))));
        }

        @Test
        void runRecordsViolationWhenBelowMin() {
            RuleContext ctx = ctxWithViolations("name", "X");
            nameSizeRule.run(ctx);
            assertEquals(1, violations(ctx).size());
        }
    }


    // ------------------------------------------------------------------
    // digits
    // ------------------------------------------------------------------

    @Nested
    class DigitsRuleTests {

        @Test
        void beanIsCreated() {
            assertNotNull(priceDigitsRule);
        }

        @Test
        void passesForPriceWithinIntegerAndFractionBounds() {
            assertTrue(priceDigitsRule.isTrue(ctx("price", new BigDecimal("12345.12"))));
        }

        @Test
        void passesForPriceWithNoFractionPart() {
            assertTrue(priceDigitsRule.isTrue(ctx("price", new BigDecimal("100"))));
        }

        @Test
        void failsWhenIntegerPartExceedsMaxDigits() {
            assertFalse(priceDigitsRule.isTrue(ctx("price", new BigDecimal("123456.00"))));
        }

        @Test
        void failsWhenFractionPartExceedsMaxDigits() {
            assertFalse(priceDigitsRule.isTrue(ctx("price", new BigDecimal("100.123"))));
        }

        @Test
        void runRecordsViolationWhenFractionExceedsMax() {
            RuleContext ctx = ctxWithViolations("price", new BigDecimal("1.999"));
            priceDigitsRule.run(ctx);
            assertEquals(1, violations(ctx).size());
        }
    }


    // ------------------------------------------------------------------
    // pattern
    // ------------------------------------------------------------------

    @Nested
    class PatternRuleTests {

        @Test
        void codePatternRuleBeanIsCreated() {
            assertNotNull(codePatternRule);
        }

        @Test
        void codePatternPassesForMatchingCode() {
            assertTrue(codePatternRule.isTrue(ctx("code", "AB-1234")));
        }

        @Test
        void codePatternFailsForLowercaseCode() {
            assertFalse(codePatternRule.isTrue(ctx("code", "ab-1234")));
        }

        @Test
        void codePatternFailsForWrongFormat() {
            assertFalse(codePatternRule.isTrue(ctx("code", "ABC-12")));
        }

        @Test
        void runRecordsViolationForNonMatchingCode() {
            RuleContext ctx = ctxWithViolations("code", "invalid");
            codePatternRule.run(ctx);
            assertEquals(1, violations(ctx).size());
        }

        @Test
        void caseInsensitivePatternRuleBeanIsCreated() {
            assertNotNull(caseInsensitivePatternRule);
        }

        @Test
        void caseInsensitivePatternPassesForLowercase() {
            assertTrue(caseInsensitivePatternRule.isTrue(ctx("code", "hello")));
        }

        @Test
        void caseInsensitivePatternPassesForUppercase() {
            assertTrue(caseInsensitivePatternRule.isTrue(ctx("code", "HELLO")));
        }

        @Test
        void caseInsensitivePatternPassesForMixedCase() {
            assertTrue(caseInsensitivePatternRule.isTrue(ctx("code", "Hello")));
        }

        @Test
        void caseInsensitivePatternFailsForNonAlpha() {
            assertFalse(caseInsensitivePatternRule.isTrue(ctx("code", "hello123")));
        }
    }


    // ------------------------------------------------------------------
    // assertEquals / assertNotEquals
    // ------------------------------------------------------------------

    @Nested
    class EqualityRuleTests {

        @Test
        void assertEqualsRuleBeanIsCreated() {
            assertNotNull(statusEqualsActiveRule);
        }

        @Test
        void assertEqualsPassesWhenStatusMatchesExpected() {
            assertTrue(statusEqualsActiveRule.isTrue(ctx("status", "ACTIVE")));
        }

        @Test
        void assertEqualsFailsWhenStatusDoesNotMatch() {
            assertFalse(statusEqualsActiveRule.isTrue(ctx("status", "INACTIVE")));
        }

        @Test
        void assertEqualsRunRecordsViolation() {
            RuleContext ctx = ctxWithViolations("status", "PENDING");
            statusEqualsActiveRule.run(ctx);
            assertEquals(1, violations(ctx).size());
        }

        @Test
        void assertNotEqualsRuleBeanIsCreated() {
            assertNotNull(statusNotDeletedRule);
        }

        @Test
        void assertNotEqualsPassesWhenStatusDiffersFromForbidden() {
            assertTrue(statusNotDeletedRule.isTrue(ctx("status", "ACTIVE")));
        }

        @Test
        void assertNotEqualsFailsWhenStatusMatchesForbidden() {
            assertFalse(statusNotDeletedRule.isTrue(ctx("status", "DELETED")));
        }

        @Test
        void assertNotEqualsRunRecordsViolation() {
            RuleContext ctx = ctxWithViolations("status", "DELETED");
            statusNotDeletedRule.run(ctx);
            assertEquals(1, violations(ctx).size());
        }
    }


    // ------------------------------------------------------------------
    // in
    // ------------------------------------------------------------------

    @Nested
    class InRuleTests {

        @Test
        void beanIsCreated() {
            assertNotNull(statusInRule);
        }

        @Test
        void passesForValueInList() {
            assertTrue(statusInRule.isTrue(ctx("status", "ACTIVE")));
        }

        @Test
        void passesForAnotherValueInList() {
            assertTrue(statusInRule.isTrue(ctx("status", "PENDING")));
        }

        @Test
        void failsForValueNotInList() {
            assertFalse(statusInRule.isTrue(ctx("status", "DELETED")));
        }

        @Test
        void runRecordsViolationForValueNotInList() {
            RuleContext ctx = ctxWithViolations("status", "UNKNOWN");
            statusInRule.run(ctx);
            assertEquals(1, violations(ctx).size());
        }
    }


    // ------------------------------------------------------------------
    // startsWith
    // ------------------------------------------------------------------

    @Nested
    class StartsWithRuleTests {

        @Test
        void beanIsCreated() {
            assertNotNull(titleStartsWithRule);
        }

        @Test
        void passesForTitleStartingWithMr() {
            assertTrue(titleStartsWithRule.isTrue(ctx("title", "Mr. Smith")));
        }

        @Test
        void passesForTitleStartingWithDr() {
            assertTrue(titleStartsWithRule.isTrue(ctx("title", "Dr. Jones")));
        }

        @Test
        void passesForTitleStartingWithProf() {
            assertTrue(titleStartsWithRule.isTrue(ctx("title", "Prof. Lee")));
        }

        @Test
        void failsForTitleWithNoRecognisedPrefix() {
            assertFalse(titleStartsWithRule.isTrue(ctx("title", "John Doe")));
        }

        @Test
        void runRecordsViolationForUnprefixedTitle() {
            RuleContext ctx = ctxWithViolations("title", "Alice");
            titleStartsWithRule.run(ctx);
            assertEquals(1, violations(ctx).size());
        }
    }


    // ------------------------------------------------------------------
    // endsWith
    // ------------------------------------------------------------------

    @Nested
    class EndsWithRuleTests {

        @Test
        void beanIsCreated() {
            assertNotNull(emailEndsWithRule);
        }

        @Test
        void passesForEmailEndingWithDotCom() {
            assertTrue(emailEndsWithRule.isTrue(ctx("email", "user@example.com")));
        }

        @Test
        void passesForEmailEndingWithDotOrg() {
            assertTrue(emailEndsWithRule.isTrue(ctx("email", "user@example.org")));
        }

        @Test
        void failsForEmailEndingWithDotNet() {
            assertFalse(emailEndsWithRule.isTrue(ctx("email", "user@example.net")));
        }

        @Test
        void runRecordsViolationForDisallowedSuffix() {
            RuleContext ctx = ctxWithViolations("email", "user@example.io");
            emailEndsWithRule.run(ctx);
            assertEquals(1, violations(ctx).size());
        }
    }


    // ------------------------------------------------------------------
    // Script expression form (expr attribute)
    // ------------------------------------------------------------------

    @Nested
    class ExprFormTests {

        @Test
        void beanIsCreated() {
            assertNotNull(minAgeByExprRule);
        }

        @Test
        void passesWhenExprReturnsValueAboveMin() {
            assertTrue(minAgeByExprRule.isTrue(ctx("age", 5)));
        }

        @Test
        void passesWhenExprReturnsValueAtBoundary() {
            assertTrue(minAgeByExprRule.isTrue(ctx("age", 0)));
        }

        @Test
        void failsWhenExprReturnsValueBelowMin() {
            assertFalse(minAgeByExprRule.isTrue(ctx("age", -1)));
        }

        @Test
        void runRecordsViolationWhenExprValueBelowMin() {
            RuleContext ctx = ctxWithViolations("age", -10);
            minAgeByExprRule.run(ctx);
            assertEquals(1, violations(ctx).size());
        }
    }


    // ------------------------------------------------------------------
    // Override form (errorCode, severity, errorMessage)
    // ------------------------------------------------------------------

    @Nested
    class OverrideFormTests {

        @Test
        void beanIsCreated() {
            assertNotNull(overriddenNotNullRule);
        }

        @Test
        void passesWhenValueIsNonNull() {
            RuleContext ctx = ctx();
            ctx.getBindings().bind("value", "present");
            assertTrue(overriddenNotNullRule.isTrue(ctx));
        }

        @Test
        void failsWhenValueIsNull() {
            RuleContext ctx = ctx();
            ctx.getBindings().bind("value", (String) null);
            assertFalse(overriddenNotNullRule.isTrue(ctx));
        }

        @Test
        void violationCarriesOverriddenErrorCode() {
            RuleContext ctx = ctxWithViolations();
            ctx.getBindings().bind("value", (String) null);
            overriddenNotNullRule.run(ctx);
            RuleViolations v = violations(ctx);
            assertEquals(1, v.size());
            assertEquals("REQUIRED_FIELD", v.getViolations().get(0).getErrorCode());
        }

        @Test
        void violationCarriesOverriddenErrorMessage() {
            RuleContext ctx = ctxWithViolations();
            ctx.getBindings().bind("value", (String) null);
            overriddenNotNullRule.run(ctx);
            RuleViolations v = violations(ctx);
            assertEquals(1, v.size());
            assertTrue(v.getViolations().get(0).getErrorMessage()
                    .contains("required"));
        }
    }


    // ------------------------------------------------------------------
    // Ruleset integration
    // ------------------------------------------------------------------

    @Nested
    class RuleSetIntegrationTests {

        @Test
        void ruleSetBeanIsCreated() {
            assertNotNull(personValidationRuleSet);
        }

        @Test
        void ruleSetHasCorrectName() {
            assertEquals("PersonValidationRuleSet", personValidationRuleSet.getName());
        }

        @Test
        void ruleSetContainsSixRules() {
            assertEquals(6, personValidationRuleSet.getRules().size());
        }

        @Test
        void ruleSetPassesForValidPerson() {
            RuleContext ctx = ctxWithViolations();
            ctx.getBindings().bind("name", "Alice");
            ctx.getBindings().bind("age", 30);
            ctx.getBindings().bind("status", "ACTIVE");
            assertDoesNotThrow(() -> personValidationRuleSet.run(ctx));
            assertTrue(violations(ctx).isEmpty());
        }

        @Test
        void ruleSetRecordsViolationForNullName() {
            RuleContext ctx = ctx();
            ctx.getBindings().bind("name", (String) null);
            ctx.getBindings().bind("age", 30);
            ctx.getBindings().bind("status", "ACTIVE");
            assertThrows(ValidationException.class, () -> personValidationRuleSet.run(ctx));
        }

        @Test
        void ruleSetRecordsViolationForAgeBelowMin() {
            RuleContext ctx = ctx();
            ctx.getBindings().bind("name", "Bob");
            ctx.getBindings().bind("age", -1);
            ctx.getBindings().bind("status", "ACTIVE");
            assertThrows(ValidationException.class, () -> personValidationRuleSet.run(ctx));
        }

        @Test
        void ruleSetRecordsViolationForAgeAboveMax() {
            RuleContext ctx = ctx();
            ctx.getBindings().bind("name", "Bob");
            ctx.getBindings().bind("age", 200);
            ctx.getBindings().bind("status", "ACTIVE");
            assertThrows(ValidationException.class, () -> personValidationRuleSet.run(ctx));
        }

        @Test
        void ruleSetRecordsViolationForInvalidStatus() {
            RuleContext ctx = ctx();
            ctx.getBindings().bind("name", "Carol");
            ctx.getBindings().bind("age", 25);
            ctx.getBindings().bind("status", "DELETED");
            assertThrows(ValidationException.class, () -> personValidationRuleSet.run(ctx));
        }

        @Test
        void ruleSetRecordsViolationForNameTooShort() {
            RuleContext ctx = ctx();
            ctx.getBindings().bind("name", "X");
            ctx.getBindings().bind("age", 25);
            ctx.getBindings().bind("status", "ACTIVE");
            assertThrows(ValidationException.class, () -> personValidationRuleSet.run(ctx));
        }

        @Test
        void ruleSetAccumulatesMultipleViolations() {
            RuleContext ctx = ctx();
            ctx.getBindings().bind("name", "X");      // fails notBlank(pass) + size(fail)
            ctx.getBindings().bind("age", -5);        // fails min
            ctx.getBindings().bind("status", "DELETED"); // fails in
            ValidationException ex = assertThrows(ValidationException.class, () -> personValidationRuleSet.run(ctx));
            assertTrue(ex.getViolations().size() >= 3);
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Bare context with no bindings. */
    private static RuleContext ctx() {
        return RuleContext.builder().standard().build();
    }

    /** Context with a single named binding pre-populated. */
    private static RuleContext ctx(String name, Object value) {
        RuleContext ctx = ctx();
        ctx.getBindings().bind(name, value);
        return ctx;
    }

    /** Context with a {@link RuleViolations} binding so {@code @Otherwise} can record violations. */
    private static RuleContext ctxWithViolations() {
        RuleContext ctx = ctx();
        ctx.getBindings().bind("ruleViolations", new RuleViolations());
        return ctx;
    }

    /** Context with one named binding and a {@link RuleViolations} binding. */
    private static RuleContext ctxWithViolations(String name, Object value) {
        RuleContext ctx = ctxWithViolations();
        ctx.getBindings().bind(name, value);
        return ctx;
    }

    /** Retrieves the {@link RuleViolations} instance from the context. */
    private static RuleViolations violations(RuleContext ctx) {
        return ctx.getBindings().getValue("ruleViolations", RuleViolations.class);
    }
}
