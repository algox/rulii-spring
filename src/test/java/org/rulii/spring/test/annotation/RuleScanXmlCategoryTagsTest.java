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
package org.rulii.spring.test.annotation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.rulii.rule.Rule;
import org.rulii.ruleflow.RuleFlow;
import org.rulii.ruleset.RuleSet;
import org.rulii.spring.annotation.RuleScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@code category} and {@code tags} on XML artifacts, and the file-level
 * {@code <r:defaults>}: an element's category replaces the default, its tags add to it, inline
 * rules read the file defaults, and nothing leaks into a file without defaults.
 *
 * @author Max Arulananthan
 * @since 2.1
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = RuleScanXmlCategoryTagsTest.Config.class)
class RuleScanXmlCategoryTagsTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @RuleScan(xmlLocations = "classpath:rules/category-tags/")
    static class Config {}

    @Autowired @Qualifier("DefaultLabelledRule") private Rule defaultLabelledRule;
    @Autowired @Qualifier("OverriddenRule") private Rule overriddenRule;
    @Autowired @Qualifier("LabelledValidation") private Rule labelledValidation;
    @Autowired @Qualifier("LabelledNotNull") private Rule labelledNotNull;
    @Autowired @Qualifier("InlineLabelled") private Rule inlineLabelled;
    @Autowired @Qualifier("LabelledSet") private RuleSet<?> labelledSet;
    @Autowired @Qualifier("LabelledFlow") private RuleFlow<?> labelledFlow;
    @Autowired @Qualifier("PlainXmlRule") private Rule plainXmlRule;

    @Test
    void fileDefaultsApplyAsTheyAre() {
        assertEquals("Pricing", defaultLabelledRule.getDefinition().getCategory());
        assertEquals(List.of("xml", "pricing"), defaultLabelledRule.getDefinition().getTags());
        assertEquals("Uses the file defaults", defaultLabelledRule.getDefinition().getDescription());
    }

    @Test
    void anElementsCategoryReplacesAndItsTagsAdd() {
        assertEquals("Fraud/Scoring", overriddenRule.getDefinition().getCategory(), "normalised, levels trimmed");
        assertEquals(List.of("xml", "pricing", "fraud"), overriddenRule.getDefinition().getTags(), "file tags first, blanks dropped, duplicates once");
    }

    @Test
    void validationRulesTakeThemToo() {
        assertEquals("Checks", labelledValidation.getDefinition().getCategory());
        assertEquals(List.of("xml", "pricing"), labelledValidation.getDefinition().getTags());
        assertEquals("Pricing", labelledNotNull.getDefinition().getCategory(), "a predefined validator takes the file default");
        assertEquals(List.of("xml", "pricing", "required"), labelledNotNull.getDefinition().getTags());
    }

    @Test
    void ruleSetsFlowsAndInlineRules() {
        assertEquals("Pricing/Sets", labelledSet.getDefinition().getCategory());
        assertEquals(List.of("xml", "pricing", "set"), labelledSet.getDefinition().getTags());
        assertEquals("Pricing", inlineLabelled.getDefinition().getCategory(), "an inline rule reads the file defaults, not its set's category");
        assertEquals(List.of("xml", "pricing"), inlineLabelled.getDefinition().getTags());
        assertEquals("Pricing", labelledFlow.getDefinition().getCategory());
        assertEquals(List.of("xml", "pricing", "flow"), labelledFlow.getDefinition().getTags());
    }

    @Test
    void defaultsStayInTheirFile() {
        assertNull(plainXmlRule.getDefinition().getCategory());
        assertEquals(Collections.emptyList(), plainXmlRule.getDefinition().getTags());
    }
}
