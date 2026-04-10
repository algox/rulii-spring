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


public class RuliiNamespaceHandler extends NamespaceHandlerSupport {

    /** Default scripting language used when an expression element carries no {@code language} attribute. */
    private String defaultLanguage = "el";

    @Override
    public void init() {
        registerBeanDefinitionParser("scripting",      new ScriptingBeanDefinitionParser(this));
        registerBeanDefinitionParser("rule",           new RuleBeanDefinitionParser(this));
        registerBeanDefinitionParser("validationRule", new ValidationRuleBeanDefinitionParser(this));
        registerBeanDefinitionParser("ruleset",        new RuleSetBeanDefinitionParser(this));

        PredefinedValidationRuleBeanDefinitionParser predefined = new PredefinedValidationRuleBeanDefinitionParser(this);

        // No extra parameters
        registerBeanDefinitionParser("notNull",         predefined);
        registerBeanDefinitionParser("notBlank",        predefined);
        registerBeanDefinitionParser("notEmpty",        predefined);
        registerBeanDefinitionParser("isNull",          predefined);
        registerBeanDefinitionParser("blank",           predefined);
        registerBeanDefinitionParser("alpha",           predefined);
        registerBeanDefinitionParser("alphaNumeric",    predefined);
        registerBeanDefinitionParser("ascii",           predefined);
        registerBeanDefinitionParser("decimal",         predefined);
        registerBeanDefinitionParser("numeric",         predefined);
        registerBeanDefinitionParser("email",           predefined);
        registerBeanDefinitionParser("url",             predefined);
        registerBeanDefinitionParser("lowerCase",       predefined);
        registerBeanDefinitionParser("upperCase",       predefined);
        registerBeanDefinitionParser("assertFalse",     predefined);
        registerBeanDefinitionParser("assertTrue",      predefined);
        registerBeanDefinitionParser("positive",        predefined);
        registerBeanDefinitionParser("positiveOrZero",  predefined);
        registerBeanDefinitionParser("negative",        predefined);
        registerBeanDefinitionParser("negativeOrZero",  predefined);
        registerBeanDefinitionParser("future",          predefined);
        registerBeanDefinitionParser("futureOrPresent", predefined);
        registerBeanDefinitionParser("past",            predefined);
        registerBeanDefinitionParser("pastOrPresent",   predefined);
        registerBeanDefinitionParser("fileExists",      predefined);

        // Numeric bounds
        registerBeanDefinitionParser("min",             predefined);
        registerBeanDefinitionParser("max",             predefined);
        registerBeanDefinitionParser("decimalMin",      predefined);
        registerBeanDefinitionParser("decimalMax",      predefined);

        // Size / digits
        registerBeanDefinitionParser("size",            predefined);
        registerBeanDefinitionParser("digits",          predefined);

        // Pattern
        registerBeanDefinitionParser("pattern",         predefined);

        // Equality
        registerBeanDefinitionParser("assertEquals",    predefined);
        registerBeanDefinitionParser("assertNotEquals", predefined);

        // Multi-value
        registerBeanDefinitionParser("startsWith",      predefined);
        registerBeanDefinitionParser("endsWith",        predefined);
        registerBeanDefinitionParser("in",              predefined);
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public void setDefaultLanguage(String defaultLanguage) {
        Assert.hasText(defaultLanguage, "defaultLanguage cannot be null or empty.");
        this.defaultLanguage = defaultLanguage;
    }
}
