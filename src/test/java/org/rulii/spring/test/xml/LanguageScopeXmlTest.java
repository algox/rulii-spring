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
import org.rulii.model.UnrulyException;
import org.rulii.rule.Rule;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that the {@code <r:scripting defaultLanguage="...">} directive is scoped to the
 * XML file that declares it and applies regardless of its position within that file.
 *
 * <p>Regression test: the default language was previously mutable state on the cached
 * namespace handler, so one file's directive leaked into every file parsed afterwards by
 * the same reader (parse-order dependent), and within a file the directive only applied
 * to elements below it.
 *
 * @author Max Arulananthan
 * @since 2.0
 */
class LanguageScopeXmlTest {

    @Test
    void scriptingDirectiveDoesNotLeakAcrossFiles() {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);

        // File A declares defaultLanguage="bogusLang" (and no rules); file B declares a rule
        // and no scripting element. Parsed by the same reader, in that order.
        reader.loadBeanDefinitions(new ClassPathResource("rules/lang-scope/lang-scope-a.xml"));
        reader.loadBeanDefinitions(new ClassPathResource("rules/lang-scope/lang-scope-b.xml"));

        // B's rule must compile under the default "el", not A's bogus language.
        Rule rule = factory.getBean("LangScopeRule", Rule.class);
        assertNotNull(rule);

        RuleContext ctx = RuleContext.builder().standard().build();
        ctx.getBindings().bind("age", 21);
        assertTrue(rule.isTrue(ctx));
    }

    @Test
    void scriptingDirectiveAppliesRegardlessOfPosition() {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);

        // The scripting directive appears AFTER the rule in the document; it must still
        // govern the whole file, so building the rule fails under "bogusLang".
        reader.loadBeanDefinitions(new ClassPathResource("rules/lang-scope/lang-scope-position.xml"));

        Exception ex = assertThrows(Exception.class, () -> factory.getBean("PositionRule", Rule.class));
        assertTrue(hasCauseWithMessage(ex, "bogusLang"),
                "rule must be built with the file's declared language, but got: " + ex);
    }

    private static boolean hasCauseWithMessage(Throwable t, String fragment) {
        while (t != null) {
            if (t instanceof UnrulyException && t.getMessage() != null && t.getMessage().contains(fragment)) return true;
            t = t.getCause();
        }
        return false;
    }
}
