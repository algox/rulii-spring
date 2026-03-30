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

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.util.Assert;

/**
 * Spring XML namespace handler for the {@code http://www.rulii.org/schema/rulii} namespace.
 *
 * <p>Registers a {@link org.springframework.beans.factory.xml.BeanDefinitionParser} for each
 * top-level element defined in {@code spring-rulii.xsd}:
 * <ul>
 *   <li>{@code <rulii:scripting>} — sets the default scripting language for subsequent elements</li>
 *   <li>{@code <rulii:rule>} — defines an inline rule backed by script conditions and actions</li>
 *   <li>{@code <rulii:validationRule>} — defines a validation rule with error metadata</li>
 *   <li>{@code <rulii:ruleset>} — defines an ordered collection of rules with lifecycle hooks</li>
 * </ul>
 *
 * <p>The {@link #defaultLanguage} field is mutated by {@link ScriptingBeanDefinitionParser} when a
 * {@code <rulii:scripting>} element is encountered and is propagated to all subsequent parsers.
 * Place {@code <rulii:scripting>} before rule/ruleset definitions to ensure the default takes effect.
 *
 * <p>Registered in {@code META-INF/spring.handlers} as:
 * <pre>
 * http\://www.rulii.org/schema/rulii=org.rulii.spring.xml.ns.RuliiNamespaceHandler
 * </pre>
 *
 * @author Max Arulananthan
 * @since 1.0
 */
public class RuliiNamespaceHandler extends NamespaceHandlerSupport {

    /** Default scripting language used when an expression element carries no {@code language} attribute. */
    private String defaultLanguage = "el";

    @Override
    public void init() {
        registerBeanDefinitionParser("scripting",      new ScriptingBeanDefinitionParser(this));
        registerBeanDefinitionParser("rule",           new RuleBeanDefinitionParser(this));
        registerBeanDefinitionParser("validationRule", new ValidationRuleBeanDefinitionParser(this));
        registerBeanDefinitionParser("ruleset",        new RuleSetBeanDefinitionParser(this));
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public void setDefaultLanguage(String defaultLanguage) {
        Assert.hasText(defaultLanguage, "defaultLanguage cannot be null or empty.");
        this.defaultLanguage = defaultLanguage;
    }
}
