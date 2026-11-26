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
import org.rulii.model.SourceDefinition;
import org.rulii.validation.Severity;
import org.rulii.validation.ValueValidationRuleBuilder;
import org.rulii.validation.rules.Validators;
import org.rulii.validation.rules.ean.EanValidationRule;
import org.rulii.validation.rules.isbn.IsbnValidationRule;
import org.rulii.validation.rules.modcheck.Mod11CheckValidationRule;
import org.rulii.validation.rules.pattern.PatternValidationRuleBuilder;
import org.rulii.validation.rules.uuid.UuidValidationRule;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Spring {@link FactoryBean} that constructs a predefined validation {@link Rule} from one of
 * the built-in validator elements (e.g. {@code <rulii:notNull>}, {@code <rulii:min>}).
 *
 * <p>The {@code type} property is set to the element's local name by the parser and drives
 * dispatch to the appropriate {@link Validators} factory method. The value to validate is
 * resolved at startup from either a named binding or a script expression via {@link ValueSource}.
 *
 * <p>All common override properties ({@code errorCode}, {@code severity}, {@code errorMessage})
 * are optional — each predefined rule supplies its own defaults when omitted. So is every setting
 * of a validator's builder ({@link #OPTIONS}): one optional attribute each, named after the
 * builder method, and left at the builder's default when the XML does not give it.
 *
 * @author Max Arulananthan
 * @since 1.0
 */
public class PredefinedValidationRuleFactoryBean implements FactoryBean<Rule>, InitializingBean, BeanNameAware, BeanFactoryAware {

    /**
     * All predefined validator element names, in registration order. This is the single
     * Java-side source of truth: {@link RuliiNamespaceHandler} registers a parser for each
     * entry, and {@code buildRuleBuilder}'s switch must cover every entry (guarded by
     * {@code PredefinedValidatorTypesTest}). The XSD declares the same elements.
     */
    public static final List<String> TYPES = List.of(
            // No extra parameters
            "notNull", "notBlank", "notEmpty", "isNull", "blank",
            "alpha", "alphaNumeric", "ascii", "decimal", "numeric",
            "email", "url", "lowerCase", "upperCase",
            "assertFalse", "assertTrue",
            "positive", "positiveOrZero", "negative", "negativeOrZero",
            "future", "futureOrPresent", "past", "pastOrPresent",
            "fileExists",
            // Numeric bounds
            "min", "max", "decimalMin", "decimalMax",
            // Size / digits
            "size", "digits",
            // Pattern
            "pattern",
            // Equality
            "assertEquals", "assertNotEquals",
            // Multi-value
            "startsWith", "endsWith", "in",
            // Added in rulii 2.2
            "uniqueElements", "normalized", "codePointLength", "uuid",
            "durationMin", "durationMax",
            "luhnCheck", "mod10Check", "mod11Check", "ean", "isbn");

    /**
     * The optional settings every validator element has, by attribute name: the settings of the
     * builder all validators share ({@code ValueValidationRuleBuilder}) that are not already one
     * of the common override attributes.
     *
     * @since 2.3
     */
    public static final List<String> COMMON_OPTIONS = List.of("failOnNull", "valueName");

    /**
     * The optional settings of each validator element beyond {@link #COMMON_OPTIONS}, by attribute
     * name: one attribute per setting of the validator's own builder, under the builder method's
     * name. An element not listed has none. A setting left out of the XML keeps the builder's default.
     * {@code PredefinedValidatorTypesTest} checks this table against the builders and the XSD.
     * ({@code caseSensitive} on {@code pattern} predates the table and is a property of its own.)
     *
     * @since 2.3
     */
    public static final Map<String, List<String>> OPTIONS = Map.ofEntries(
            Map.entry("alpha",          List.of("allowSpace")),
            Map.entry("alphaNumeric",    List.of("allowSpace")),
            Map.entry("decimal",         List.of("allowSpace")),
            Map.entry("numeric",         List.of("allowSpace")),
            Map.entry("decimalMin",      List.of("inclusive")),
            Map.entry("decimalMax",      List.of("inclusive")),
            Map.entry("email",           List.of("allowLocal", "allowTopLevelDomain", "anyDomain", "pattern", "flags")),
            Map.entry("url",             List.of("protocol", "host", "port", "regexp", "flags")),
            Map.entry("pattern",         List.of("flags")),
            Map.entry("normalized",      List.of("form")),
            Map.entry("codePointLength", List.of("normalization")),
            Map.entry("durationMin",     List.of("inclusive")),
            Map.entry("durationMax",     List.of("inclusive")),
            Map.entry("uuid",            List.of("allowEmpty", "allowNil", "versions", "variants", "letterCase")),
            Map.entry("luhnCheck",       List.of("startIndex", "endIndex", "checkDigitIndex", "ignoreNonDigitCharacters")),
            Map.entry("mod10Check",      List.of("multiplier", "weight", "startIndex", "endIndex", "checkDigitIndex",
                                                 "ignoreNonDigitCharacters")),
            Map.entry("mod11Check",      List.of("threshold", "startIndex", "endIndex", "checkDigitIndex",
                                                 "ignoreNonDigitCharacters", "treatCheck10As", "treatCheck11As",
                                                 "processingDirection")),
            Map.entry("ean",             List.of("type")),
            Map.entry("isbn",            List.of("type")));

    /** The names a {@code flags} attribute accepts: the {@link Pattern} flag constants. */
    private static final Map<String, Integer> PATTERN_FLAGS = Map.of(
            "UNIX_LINES", Pattern.UNIX_LINES,
            "CASE_INSENSITIVE", Pattern.CASE_INSENSITIVE,
            "COMMENTS", Pattern.COMMENTS,
            "MULTILINE", Pattern.MULTILINE,
            "LITERAL", Pattern.LITERAL,
            "DOTALL", Pattern.DOTALL,
            "UNICODE_CASE", Pattern.UNICODE_CASE,
            "CANON_EQ", Pattern.CANON_EQ,
            "UNICODE_CHARACTER_CLASS", Pattern.UNICODE_CHARACTER_CLASS);

    private String type;
    private String name;
    private String description;
    private String category;
    private List<String> tags = new ArrayList<>();
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

    // Optional settings, by attribute name (see OPTIONS)
    private Map<String, String> options = Map.of();

    private Rule rule;
    private String beanName;
    private BeanFactory beanFactory;

    public PredefinedValidationRuleFactoryBean() {
        super();
    }

    @Override
    public void afterPropertiesSet() {
        if (type == null) throw new UnrulyException("Predefined validation rule '" + name + "': type cannot be null.");
        if (valueSource == null) throw new UnrulyException("Predefined validation rule '" + name + "': valueSource cannot be null.");

        Function<?> fn = buildValueFunction();
        ValueValidationRuleBuilder<?, ?> vb = buildRuleBuilder(fn);

        vb.name(name).description(description);
        vb.category(category);
        vb.tags(tags.toArray(new String[0]));

        if (StringUtils.hasText(errorCode)) vb.errorCode(errorCode);
        if (StringUtils.hasText(severity)) vb.severity(Severity.valueOf(severity.toUpperCase(Locale.ROOT)));
        if (StringUtils.hasText(errorMessage)) vb.message(errorMessage);

        // The settings every validator shares (COMMON_OPTIONS).
        option("failOnNull", v -> vb.failOnNull(toBoolean(v)));
        option("valueName", vb::valueName);

        // The bean definition knows the XML file and line; the artifact reports them as its source.
        SourceDefinition source = BeanSources.sourceOf(beanFactory, beanName);
        if (source != null) vb.source(source);
        rule = vb.build();
    }

    private Function<?> buildValueFunction() {
        if (valueSource.isBinding()) {
            return Validators.binding(valueSource.getBindingName());
        }

        // ValueSource guarantees a non-null script with a non-null expression when not a binding;
        // a missing value source is reported at XML parse time with the source location.
        return valueSource.getScript().toFunction(defaultLanguage);
    }

    @SuppressWarnings("unchecked")
    private ValueValidationRuleBuilder<?, ?> buildRuleBuilder(Function<?> fn) {
        return switch (type) {
            case "notNull"         -> Validators.notNull(fn);
            case "notBlank"        -> Validators.notBlank(fn);
            case "notEmpty"        -> Validators.notEmpty(fn);
            case "isNull"          -> Validators.isNull(fn);
            case "blank"           -> Validators.blank(fn);
            case "alpha"           -> { var b = Validators.alpha(fn); flag("allowSpace", b::allowSpace); yield b; }
            case "alphaNumeric"    -> { var b = Validators.alphaNumeric(fn); flag("allowSpace", b::allowSpace); yield b; }
            case "ascii"           -> Validators.ascii(fn);
            case "decimal"         -> { var b = Validators.decimal(fn); flag("allowSpace", b::allowSpace); yield b; }
            case "numeric"         -> { var b = Validators.numeric(fn); flag("allowSpace", b::allowSpace); yield b; }
            case "email"           -> buildEmailBuilder(fn);
            case "url"             -> buildUrlBuilder(fn);
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
            case "decimalMin"      -> { var b = Validators.decimalMin(fn, new BigDecimal(min)); option("inclusive", v -> b.inclusive(toBoolean(v))); yield b; }
            case "decimalMax"      -> { var b = Validators.decimalMax(fn, new BigDecimal(max)); option("inclusive", v -> b.inclusive(toBoolean(v))); yield b; }
            case "size"            -> Validators.size(fn, Integer.parseInt(min), Integer.parseInt(max));
            case "digits"          -> Validators.digits(fn, Integer.parseInt(maxIntegerDigits), Integer.parseInt(maxFractionDigits));
            case "pattern"         -> buildPatternBuilder(fn);
            case "assertEquals"    -> Validators.assertEquals(fn, value);
            case "assertNotEquals" -> Validators.assertNotEquals(fn, value);
            case "startsWith"      -> Validators.startsWith(fn, items.toArray(String[]::new));
            case "endsWith"        -> Validators.endsWith(fn, items.toArray(String[]::new));
            case "in"              -> Validators.in(fn, items);
            case "uniqueElements"  -> Validators.uniqueElements(fn);
            case "normalized"      -> { var b = Validators.normalized(fn); option("form", v -> b.form(Normalizer.Form.valueOf(v))); yield b; }
            case "codePointLength" -> buildCodePointLengthBuilder(fn);
            case "uuid"            -> buildUuidBuilder(fn);
            case "durationMin"     -> { var b = Validators.durationMin(fn, required("min", min, Duration::parse)); option("inclusive", v -> b.inclusive(toBoolean(v))); yield b; }
            case "durationMax"     -> { var b = Validators.durationMax(fn, required("max", max, Duration::parse)); option("inclusive", v -> b.inclusive(toBoolean(v))); yield b; }
            case "luhnCheck"       -> buildLuhnCheckBuilder(fn);
            case "mod10Check"      -> buildMod10CheckBuilder(fn);
            case "mod11Check"      -> buildMod11CheckBuilder(fn);
            case "ean"             -> { var b = Validators.ean(fn); option("type", v -> b.type(EanValidationRule.Type.valueOf(v))); yield b; }
            case "isbn"            -> { var b = Validators.isbn(fn); option("type", v -> b.type(IsbnValidationRule.Type.valueOf(v))); yield b; }
            default -> throw new UnrulyException("Unknown predefined validator type: '" + type + "'.");
        };
    }

    private PatternValidationRuleBuilder buildPatternBuilder(Function<?> fn) {
        PatternValidationRuleBuilder builder = Validators.pattern(fn, pattern);
        builder.caseSensitive(caseSensitive);
        option("flags", v -> builder.flags(toFlags(v)));
        return builder;
    }

    private ValueValidationRuleBuilder<?, ?> buildEmailBuilder(Function<?> fn) {
        var builder = Validators.email(fn);
        flag("allowLocal", builder::allowLocal);
        flag("allowTopLevelDomain", builder::allowTopLevelDomain);
        flag("anyDomain", builder::anyDomain);
        option("pattern", builder::pattern);
        option("flags", v -> builder.flags(toFlags(v)));
        return builder;
    }

    private ValueValidationRuleBuilder<?, ?> buildUrlBuilder(Function<?> fn) {
        var builder = Validators.url(fn);
        option("protocol", builder::protocol);
        option("host", builder::host);
        option("port", v -> builder.port(Integer.parseInt(v)));
        option("regexp", builder::regexp);
        option("flags", v -> builder.flags(toFlags(v)));
        return builder;
    }

    private ValueValidationRuleBuilder<?, ?> buildCodePointLengthBuilder(Function<?> fn) {
        var builder = Validators.codePointLength(fn, required("min", min, Integer::parseInt), required("max", max, Integer::parseInt));
        option("normalization", v -> builder.normalization(Normalizer.Form.valueOf(v)));
        return builder;
    }

    private ValueValidationRuleBuilder<?, ?> buildUuidBuilder(Function<?> fn) {
        var builder = Validators.uuid(fn);
        option("allowEmpty", v -> builder.allowEmpty(toBoolean(v)));
        option("allowNil", v -> builder.allowNil(toBoolean(v)));
        option("versions", v -> builder.versions(toInts(v)));
        option("variants", v -> builder.variants(toInts(v)));
        option("letterCase", v -> builder.letterCase(UuidValidationRule.LetterCase.valueOf(v)));
        return builder;
    }

    private ValueValidationRuleBuilder<?, ?> buildLuhnCheckBuilder(Function<?> fn) {
        var builder = Validators.luhnCheck(fn);
        option("startIndex", v -> builder.startIndex(Integer.parseInt(v)));
        option("endIndex", v -> builder.endIndex(Integer.parseInt(v)));
        option("checkDigitIndex", v -> builder.checkDigitIndex(Integer.parseInt(v)));
        option("ignoreNonDigitCharacters", v -> builder.ignoreNonDigitCharacters(toBoolean(v)));
        return builder;
    }

    private ValueValidationRuleBuilder<?, ?> buildMod10CheckBuilder(Function<?> fn) {
        var builder = Validators.mod10Check(fn);
        option("multiplier", v -> builder.multiplier(Integer.parseInt(v)));
        option("weight", v -> builder.weight(Integer.parseInt(v)));
        option("startIndex", v -> builder.startIndex(Integer.parseInt(v)));
        option("endIndex", v -> builder.endIndex(Integer.parseInt(v)));
        option("checkDigitIndex", v -> builder.checkDigitIndex(Integer.parseInt(v)));
        option("ignoreNonDigitCharacters", v -> builder.ignoreNonDigitCharacters(toBoolean(v)));
        return builder;
    }

    private ValueValidationRuleBuilder<?, ?> buildMod11CheckBuilder(Function<?> fn) {
        var builder = Validators.mod11Check(fn);
        option("threshold", v -> builder.threshold(Integer.parseInt(v)));
        option("startIndex", v -> builder.startIndex(Integer.parseInt(v)));
        option("endIndex", v -> builder.endIndex(Integer.parseInt(v)));
        option("checkDigitIndex", v -> builder.checkDigitIndex(Integer.parseInt(v)));
        option("ignoreNonDigitCharacters", v -> builder.ignoreNonDigitCharacters(toBoolean(v)));
        option("treatCheck10As", v -> builder.treatCheck10As(toChar(v)));
        option("treatCheck11As", v -> builder.treatCheck11As(toChar(v)));
        option("processingDirection", v -> builder.processingDirection(Mod11CheckValidationRule.ProcessingDirection.valueOf(v)));
        return builder;
    }

    /**
     * Applies an optional setting when the XML gave it; a setting left out keeps the builder's default.
     */
    private void option(String attribute, Consumer<String> setter) {
        String value = options.get(attribute);
        if (!StringUtils.hasText(value)) return;

        try {
            setter.accept(value.strip());
        } catch (RuntimeException e) {
            throw invalid(attribute, value, e);
        }
    }

    /** Calls a no-argument switch of the builder when the XML set the attribute to true. */
    private void flag(String attribute, Runnable setter) {
        option(attribute, v -> { if (toBoolean(v)) setter.run(); });
    }

    private <T> T required(String attribute, String value, java.util.function.Function<String, T> converter) {
        try {
            return converter.apply(value.strip());
        } catch (RuntimeException e) {
            throw invalid(attribute, value, e);
        }
    }

    private UnrulyException invalid(String attribute, String value, RuntimeException cause) {
        return new UnrulyException("Predefined validation rule '" + name + "' (" + type + "): invalid value '" + value
                + "' for attribute '" + attribute + "'.", cause);
    }

    private static boolean toBoolean(String value) {
        if ("true".equals(value) || "1".equals(value)) return true;
        if ("false".equals(value) || "0".equals(value)) return false;
        throw new IllegalArgumentException("Not a boolean: " + value);
    }

    private static int[] toInts(String value) {
        return Arrays.stream(value.split(",")).map(String::strip).mapToInt(Integer::parseInt).toArray();
    }

    private static char toChar(String value) {
        if (value.length() != 1) throw new IllegalArgumentException("Not a single character: " + value);
        return value.charAt(0);
    }

    /** Combines the named {@link Pattern} flags, e.g. {@code MULTILINE,DOTALL}. */
    private static int toFlags(String value) {
        int result = 0;

        for (String flagName : value.split(",")) {
            Integer flag = PATTERN_FLAGS.get(flagName.strip());
            if (flag == null) throw new IllegalArgumentException("Unknown regular expression flag: " + flagName.strip());
            result |= flag;
        }

        return result;
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

    public void setCategory(String category) {
        this.category = category;
    }

    public void setTags(List<String> tags) {
        this.tags = tags != null ? tags : new ArrayList<>();
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

    /**
     * Sets the optional settings of the validator, by attribute name ({@link #OPTIONS}).
     *
     * @param options the settings given in the XML; null for none.
     * @since 2.3
     */
    public void setOptions(Map<String, String> options) {
        this.options = options != null ? options : Map.of();
    }

    @Override
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}
