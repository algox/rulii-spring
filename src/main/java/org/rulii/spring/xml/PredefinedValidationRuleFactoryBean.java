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
package org.rulii.spring.xml;

import org.rulii.model.UnrulyException;
import org.rulii.model.function.Function;
import org.rulii.rule.Rule;
import org.rulii.script.Script;
import org.rulii.validation.Severity;
import org.rulii.validation.ValueValidationRuleBuilder;
import org.rulii.validation.rules.Validators;
import org.rulii.validation.rules.pattern.PatternValidationRuleBuilder;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * Spring {@link FactoryBean} that constructs a predefined validation {@link Rule} from one of
 * the built-in validator elements (e.g. {@code <rulii:notNull>}, {@code <rulii:min>}).
 *
 * <p>The {@code type} property is set to the element's local name by the parser and drives
 * dispatch to the appropriate {@link Validators} factory method. The value to validate is
 * resolved at startup from either a named binding or a script expression via {@link ValueSource}.
 *
 * <p>All common override properties ({@code errorCode}, {@code severity}, {@code errorMessage})
 * are optional — each predefined rule supplies its own defaults when omitted.
 *
 * @author Max Arulananthan
 * @since 1.0
 */
public class PredefinedValidationRuleFactoryBean implements FactoryBean<Rule>, InitializingBean {

    private String type;
    private String name;
    private String description;
    private String defaultLanguage;
    private ValueSource valueSource;

    // Common overrides
    private String errorCode;
    private String severity;
    private String errorMessage;

    // Numeric bounds (min / max / decimalMin / decimalMax / size)
    private String min;
    private String max;

    // Digits
    private String maxIntegerDigits;
    private String maxFractionDigits;

    // Pattern
    private String pattern;
    private boolean caseSensitive = true;

    // Equality (assertEquals / assertNotEquals)
    private String value;

    // Multi-value (startsWith / endsWith / in)
    private List<String> items;

    private Rule rule;

    public PredefinedValidationRuleFactoryBean() {}

    @Override
    public void afterPropertiesSet() {
        if (type == null) throw new UnrulyException("Predefined validation rule '" + name + "': type cannot be null.");
        if (valueSource == null) throw new UnrulyException("Predefined validation rule '" + name + "': valueSource cannot be null.");

        Function<?> fn = buildValueFunction();
        ValueValidationRuleBuilder<?, ?> vb = buildRuleBuilder(fn);

        vb.name(name).description(description);

        if (StringUtils.hasText(errorCode)) vb.errorCode(errorCode);
        if (StringUtils.hasText(severity)) vb.severity(Severity.valueOf(severity.toUpperCase()));
        if (StringUtils.hasText(errorMessage)) vb.message(errorMessage);

        rule = vb.build();
    }

    private Function<?> buildValueFunction() {
        if (valueSource.isBinding()) {
            return Validators.binding(valueSource.getBindingName());
        }

        ScriptExpression script = valueSource.getScript();
        if (script == null || script.getExpression() == null) {
            throw new UnrulyException("Predefined validation rule '" + name
                    + "': must supply either a binding attribute or a script expression (expr attribute or element body).");
        }

        return Function.builder().build(
                Script.builder().build(script.resolveLanguage(defaultLanguage), script.getExpression()));
    }

    @SuppressWarnings("unchecked")
    private ValueValidationRuleBuilder<?, ?> buildRuleBuilder(Function<?> fn) {
        return switch (type) {
            case "notNull"         -> Validators.notNull(fn);
            case "notBlank"        -> Validators.notBlank(fn);
            case "notEmpty"        -> Validators.notEmpty(fn);
            case "isNull"          -> Validators.isNull(fn);
            case "blank"           -> Validators.blank(fn);
            case "alpha"           -> Validators.alpha(fn);
            case "alphaNumeric"    -> Validators.alphaNumeric(fn);
            case "ascii"           -> Validators.ascii(fn);
            case "decimal"         -> Validators.decimal(fn);
            case "numeric"         -> Validators.numeric(fn);
            case "email"           -> Validators.email(fn);
            case "url"             -> Validators.url(fn);
            case "lowerCase"       -> Validators.lowerCase(fn);
            case "upperCase"       -> Validators.upperCase(fn);
            case "assertFalse"     -> Validators.assertFalse(fn);
            case "assertTrue"      -> Validators.assertTrue(fn);
            case "positive"        -> Validators.positive(fn);
            case "positiveOrZero"  -> Validators.positiveOrZero(fn);
            case "negative"        -> Validators.negative(fn);
            case "negativeOrZero"  -> Validators.negativeOrZero(fn);
            case "future"          -> Validators.future(fn);
            case "futureOrPresent" -> Validators.futureOrPresent(fn);
            case "past"            -> Validators.past(fn);
            case "pastOrPresent"   -> Validators.pastOrPresent(fn);
            case "fileExists"      -> Validators.fileExists(fn);
            case "min"             -> Validators.min(fn, Long.parseLong(min));
            case "max"             -> Validators.max(fn, Long.parseLong(max));
            case "decimalMin"      -> Validators.decimalMin(fn, new BigDecimal(min));
            case "decimalMax"      -> Validators.decimalMax(fn, new BigDecimal(max));
            case "size"            -> Validators.size(fn, Integer.parseInt(min), Integer.parseInt(max));
            case "digits"          -> Validators.digits(fn, Integer.parseInt(maxIntegerDigits), Integer.parseInt(maxFractionDigits));
            case "pattern"         -> buildPatternBuilder(fn);
            case "assertEquals"    -> Validators.assertEquals(fn, value);
            case "assertNotEquals" -> Validators.assertNotEquals(fn, value);
            case "startsWith"      -> Validators.startsWith(fn, items.toArray(String[]::new));
            case "endsWith"        -> Validators.endsWith(fn, items.toArray(String[]::new));
            case "in"              -> Validators.in(fn, items);
            default -> throw new UnrulyException("Unknown predefined validator type: '" + type + "'.");
        };
    }

    private PatternValidationRuleBuilder buildPatternBuilder(Function<?> fn) {
        PatternValidationRuleBuilder builder = Validators.pattern(fn, pattern);
        builder.caseSensitive(caseSensitive);
        return builder;
    }

    @Override
    public Rule getObject() { return rule; }

    @Override
    public Class<?> getObjectType() { return Rule.class; }

    public void setType(String type) {
        this.type = type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }

    public void setValueSource(ValueSource valueSource) {
        this.valueSource = valueSource;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setMin(String min) {
        this.min = min;
    }

    public void setMax(String max) {
        this.max = max;
    }

    public void setMaxIntegerDigits(String maxIntegerDigits) {
        this.maxIntegerDigits = maxIntegerDigits;
    }

    public void setMaxFractionDigits(String maxFractionDigits) {
        this.maxFractionDigits = maxFractionDigits;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setItems(List<String> items) {
        this.items = items;
    }
}
