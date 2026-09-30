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
import org.rulii.spring.xml.LineTrackingDocumentLoader;
import org.rulii.spring.xml.XmlSource;
import org.rulii.spring.xml.XmlSourceExtractor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that every bean definition the rulii parsers register carries an
 * {@link XmlSource} (file and line) when the reader uses the
 * {@link LineTrackingDocumentLoader} and {@link XmlSourceExtractor}, and that a plain
 * reader still records the file (via the resource description) but no line.
 *
 * @author Max Arulananthan
 * @since 2.1
 */
class XmlSourceTest {

    private static final ClassPathResource FIXTURE = new ClassPathResource("rules/xml-source-test.xml");

    private DefaultListableBeanFactory loadWithLineTracking() {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
        reader.setDocumentLoader(new LineTrackingDocumentLoader());
        reader.setSourceExtractor(new XmlSourceExtractor());
        reader.loadBeanDefinitions(FIXTURE);
        return factory;
    }

    private static XmlSource sourceOf(DefaultListableBeanFactory factory, String beanName) {
        BeanDefinition definition = factory.getBeanDefinition(beanName);
        XmlSource source = assertInstanceOf(XmlSource.class, definition.getSource(), beanName);
        assertNotNull(source.resource(), beanName);
        assertTrue(source.resource().getDescription().contains("xml-source-test.xml"), source.describe());
        assertTrue(definition.getResourceDescription().contains("xml-source-test.xml"), beanName);
        return source;
    }

    @Test
    void topLevelDefinitionsCarryFileAndLine() {
        DefaultListableBeanFactory factory = loadWithLineTracking();

        assertEquals(14, sourceOf(factory, "SourceRule").line());
        assertEquals(17, sourceOf(factory, "MultiLineRule").line(), "the line the start tag begins on");
        assertEquals(23, sourceOf(factory, "SourceValidationRule").line());
        assertEquals(25, sourceOf(factory, "SourceNotNullRule").line());
        assertEquals(27, sourceOf(factory, "SourceRuleSet").line());
        assertEquals(39, sourceOf(factory, "SourceFlow").line());
        assertEquals(43, sourceOf(factory, "plainBean").line(), "Spring's own <bean> gets a source too");
    }

    @Test
    void inlineRuleSetMembersCarryFileAndLine() {
        DefaultListableBeanFactory factory = loadWithLineTracking();

        assertEquals(29, sourceOf(factory, "SourceInlineRule").line());
        assertEquals(30, sourceOf(factory, "SourceInlineValidationRule").line());
        assertEquals(31, sourceOf(factory, "SourceInlineNotBlankRule").line());

        // <class-ref> registers two auto-named beans: the rule instance and its wrapper
        List<Integer> classRefLines = new ArrayList<>();
        for (String name : factory.getBeanDefinitionNames()) {
            String className = factory.getBeanDefinition(name).getBeanClassName();
            if (className != null && (className.endsWith("RangeCheckRule") || className.endsWith("RuleFromInstanceFactoryBean"))) {
                classRefLines.add(sourceOf(factory, name).line());
            }
        }
        assertEquals(List.of(32, 32), classRefLines);
    }

    @Test
    void describeReadsAsFileAndLine() {
        XmlSource source = sourceOf(loadWithLineTracking(), "SourceRule");
        assertTrue(source.describe().endsWith("xml-source-test.xml]:14"), source.describe());
        assertEquals(source.describe(), new XmlSource(source.resource(), 14).describe());
        assertFalse(new XmlSource(source.resource(), null).describe().contains(":"), "no line, no suffix");
    }

    @Test
    void plainReaderKeepsTheFileButNoLine() {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        new XmlBeanDefinitionReader(factory).loadBeanDefinitions(FIXTURE);

        for (String name : List.of("SourceRule", "SourceInlineRule", "SourceRuleSet")) {
            BeanDefinition definition = factory.getBeanDefinition(name);
            assertNull(definition.getSource(), name + ": Spring's default extractor records no source");
            assertTrue(definition.getResourceDescription().contains("xml-source-test.xml"), name);
        }
    }

    @Test
    void loaderRecordsTheLineOfEveryElement() throws Exception {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <root>
                    <a/>
                    <!-- a comment -->
                    <b><c>text</c>
                        <d
                            attr="x"/>
                    </b>
                    <![CDATA[ignored]]><e/>
                </root>
                """;

        Document document = new LineTrackingDocumentLoader().loadDocument(
                new InputSource(new StringReader(xml)), null, null, XmlBeanDefinitionReader.VALIDATION_NONE, true);

        assertNotNull(LineTrackingDocumentLoader.lineOf(document.getDocumentElement()), "the root element is recorded too (approximately: the prolog has no SAX events)");
        assertEquals(List.of(3, 5, 5, 6, 9), linesOf(document.getDocumentElement().getElementsByTagName("*")));
        assertNull(LineTrackingDocumentLoader.lineOf(null));
        assertNull(LineTrackingDocumentLoader.lineOf(document.createElement("fresh")), "elements the loader did not see have no line");
    }

    private static List<Integer> linesOf(NodeList nodes) {
        List<Integer> lines = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) lines.add(LineTrackingDocumentLoader.lineOf((Element) nodes.item(i)));
        return lines;
    }
}
