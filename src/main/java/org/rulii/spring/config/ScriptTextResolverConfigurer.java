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
package org.rulii.spring.config;

import org.rulii.script.ScriptProcessorManager;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.env.Environment;

import java.util.function.UnaryOperator;

/**
 * Installs a Spring-backed script-text resolver on the process-wide
 * {@link ScriptProcessorManager}, so {@code ${property:default}} placeholders in script
 * text are resolved against the application environment before compilation — for every
 * language and both declaration styles (XML rule files and programmatic
 * {@code Script.builder()} usage), with the same semantics as {@code @Value}: defaults
 * via {@code ${key:default}}, a missing key without a default fails fast naming the
 * placeholder, and {@code \\${...}} escapes to a literal.
 *
 * <p>Registered as a {@link BeanFactoryPostProcessor} so the resolver is installed
 * <em>before any singleton instantiates</em> — XML-declared rules compile their scripts
 * during bean creation, so a later hook would be exposed to bean-ordering races.
 *
 * <p>Resolution delegates to the bean factory's embedded value resolver (the exact
 * mechanism behind {@code @Value}, contributed by
 * {@code PropertySourcesPlaceholderConfigurer}) when present, falling back to
 * {@link Environment#resolveRequiredPlaceholders}. Script text without {@code ${} } is
 * passed through untouched (fast path).
 *
 * <p>The manager is JVM-global (see {@code RuleConfig#scriptProcessorManager}): with
 * multiple application contexts the most recently refreshed context's resolver wins.
 * When this context closes, the resolver is reset to identity — unless another context
 * has installed its own in the meantime.
 *
 * <p><strong>Customizing:</strong> to change resolution semantics (e.g. lenient handling
 * of unresolvable placeholders), subclass and override {@link #createResolver}; register
 * the subclass as a <em>static</em> {@code @Bean} (standard BeanFactoryPostProcessor
 * hygiene) — the default backs off via {@code @ConditionalOnMissingBean}. Subclassing
 * keeps the reset-on-close lifecycle handling for free. To disable placeholder
 * resolution entirely, set {@code rulii.scripts.resolvePlaceholders=false}.
 *
 * @author Max Arulananthan
 * @since 2.0
 */
public class ScriptTextResolverConfigurer implements BeanFactoryPostProcessor, DisposableBean {

    private UnaryOperator<String> installedResolver;

    public ScriptTextResolverConfigurer() {
        super();
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        UnaryOperator<String> resolver = createResolver(beanFactory, beanFactory.getBean(Environment.class));
        this.installedResolver = resolver;
        ScriptProcessorManager.getInstance().setScriptTextResolver(resolver);
    }

    /**
     * Creates the resolver applied to every script's source text before compilation.
     * The default delegates to the bean factory's embedded value resolver (the mechanism
     * behind {@code @Value}) when present, falling back to
     * {@link Environment#resolveRequiredPlaceholders}; text without {@code ${} } passes
     * through untouched. Override to change resolution semantics.
     *
     * @param beanFactory the bean factory being post-processed
     * @param environment the application environment
     * @return the resolver to install; must not be null
     */
    protected UnaryOperator<String> createResolver(ConfigurableListableBeanFactory beanFactory, Environment environment) {
        return text -> {
            // Fast path: the vast majority of scripts carry no placeholders
            // (an escaped \${...} still contains "${" and is handled by the resolver).
            if (!text.contains("${")) return text;
            if (beanFactory.hasEmbeddedValueResolver()) return beanFactory.resolveEmbeddedValue(text);
            return environment.resolveRequiredPlaceholders(text);
        };
    }

    @Override
    public void destroy() {
        // Reset only if our resolver is still the active one - a newer context may have
        // installed its own, which must not be clobbered by this context's shutdown.
        if (installedResolver != null) {
            ScriptProcessorManager.getInstance().clearScriptTextResolver(installedResolver);
            installedResolver = null;
        }
    }
}
