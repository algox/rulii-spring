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
package org.rulii.spring.test.config;

import org.junit.jupiter.api.Test;
import org.rulii.context.RuleContext;
import org.rulii.registry.RuleRegistry;
import org.rulii.rule.Rule;
import org.rulii.script.Script;
import org.rulii.script.ScriptProcessorManager;
import org.rulii.spring.annotation.RuleScan;
import org.rulii.spring.script.el.SpelScriptProcessor;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies {@code ${property:default}} placeholder resolution in script text, with
 * {@code @Value}-equivalent semantics: environment-sourced values, {@code :default}
 * fallbacks, fail-fast on missing keys, and {@code \\${...}} escaping — for both
 * XML-declared expressions and programmatic {@code Script.builder()} usage.
 *
 * <p>Each test manages its own context: the resolver on the JVM-global
 * {@link ScriptProcessorManager} is installed at context refresh (via a
 * BeanFactoryPostProcessor, before any rule bean compiles) and reset on close, so
 * self-managed contexts keep the tests deterministic regardless of suite ordering.
 *
 * @author Max Arulananthan
 * @since 2.0
 */
class ScriptPlaceholderTest {

    // Plain @Configuration (not @SpringBootConfiguration) so other tests in this package
    // using default configuration resolution do not pick this class up.
    @Configuration
    @EnableAutoConfiguration
    @RuleScan(xmlLocations = "classpath:rules/placeholder/")
    static class Config {}

    private AnnotationConfigApplicationContext newContext() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.getEnvironment().getPropertySources().addFirst(new MapPropertySource("placeholder-test",
                Map.of("order.minTotal", "50", "discount.cap", "20")));
        ctx.register(Config.class);
        ctx.refresh();
        return ctx;
    }

    private RuleContext ruleCtx(String name, Object value) {
        RuleContext ctx = RuleContext.builder().standard().build();
        ctx.getBindings().bind(name, value);
        return ctx;
    }

    @Test
    void xmlPlaceholderResolvesFromEnvironment() {
        try (AnnotationConfigApplicationContext context = newContext()) {
            Rule rule = context.getBean(RuleRegistry.class).getRule("MinTotalRule");

            // order.minTotal=50 from the environment overrides the :100 default
            assertTrue(rule.isTrue(ruleCtx("total", 50)));
            assertFalse(rule.isTrue(ruleCtx("total", 49)));
        }
    }

    @Test
    void xmlPlaceholderDefaultIsUsedWhenKeyAbsent() {
        try (AnnotationConfigApplicationContext context = newContext()) {
            Rule rule = context.getBean(RuleRegistry.class).getRule("DefaultedRule");

            assertTrue(rule.isTrue(ruleCtx("age", 18)));
            assertFalse(rule.isTrue(ruleCtx("age", 17)));
        }
    }

    @Test
    void escapedPlaceholderStaysLiteral() {
        try (AnnotationConfigApplicationContext context = newContext()) {
            Rule rule = context.getBean(RuleRegistry.class).getRule("EscapedRule");

            assertTrue(rule.isTrue(ruleCtx("name", "${literal}")));
        }
    }

    @Test
    void programmaticScriptResolvesPlaceholders() {
        try (AnnotationConfigApplicationContext context = newContext()) {
            Script<Boolean> script = Script.builder().build("el", "#ctx.value <= ${discount.cap}");

            SpelScriptProcessor processor = new SpelScriptProcessor("el", "ctx");
            assertEquals(Boolean.TRUE, processor.evaluate(script, ruleCtx("value", 20)));
            assertEquals(Boolean.FALSE, processor.evaluate(script, ruleCtx("value", 21)));
        }
    }

    @Test
    void missingKeyWithoutDefaultFailsFast() {
        try (AnnotationConfigApplicationContext context = newContext()) {
            Exception ex = assertThrows(Exception.class,
                    () -> Script.builder().build("el", "${no.such.key.for.this.test} == 1"));
            assertTrue(ex.getMessage().contains("no.such.key.for.this.test"),
                    "the failure must name the unresolvable placeholder, but was: " + ex.getMessage());
        }
    }

    @Test
    void resolverIsResetWhenContextCloses() {
        AnnotationConfigApplicationContext context = newContext();
        assertEquals("20", ScriptProcessorManager.getInstance().resolveScriptText("${discount.cap}"));

        context.close();

        assertEquals("${discount.cap}", ScriptProcessorManager.getInstance().resolveScriptText("${discount.cap}"),
                "closing the context must reset the resolver to identity");
    }

    // Plain @Configuration (not @SpringBootConfiguration) so other tests in this package
    // using default configuration resolution do not pick this class up.
    @Configuration
    @EnableAutoConfiguration
    static class NoRulesConfig {}

    @Test
    void resolutionCanBeDisabledByProperty() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("placeholder-test",
                Map.of("rulii.scripts.resolvePlaceholders", "false")));
        context.register(NoRulesConfig.class);
        context.refresh();

        try (context) {
            assertEquals(0, context.getBeanNamesForType(org.rulii.spring.config.ScriptTextResolverConfigurer.class).length,
                    "the configurer must back off when the property disables it");
            assertEquals("${anything}", ScriptProcessorManager.getInstance().resolveScriptText("${anything}"),
                    "script text must pass through untouched when resolution is disabled");
        }
    }

    /** Lenient resolver: replaces a known marker, leaves everything else untouched. */
    static class LenientConfigurer extends org.rulii.spring.config.ScriptTextResolverConfigurer {
        public LenientConfigurer() {
            super();
        }

        @Override
        protected java.util.function.UnaryOperator<String> createResolver(
                org.springframework.beans.factory.config.ConfigurableListableBeanFactory beanFactory,
                org.springframework.core.env.Environment environment) {
            return text -> text.replace("${magic}", "42");
        }
    }

    @Configuration
    @EnableAutoConfiguration
    static class CustomConfigurerConfig {

        @org.springframework.context.annotation.Bean
        public static LenientConfigurer lenientConfigurer() {
            return new LenientConfigurer();
        }
    }

    @Test
    void userConfigurerBeanOverridesTheDefault() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(CustomConfigurerConfig.class)) {
            assertEquals("value == 42", ScriptProcessorManager.getInstance().resolveScriptText("value == ${magic}"),
                    "the custom resolver must be active");
            assertEquals("${unresolvable}", ScriptProcessorManager.getInstance().resolveScriptText("${unresolvable}"),
                    "the default strict resolver must have backed off (lenient resolver leaves unknowns untouched)");
        }
    }
}
