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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.xml.DefaultDocumentLoader;
import org.springframework.util.FileCopyUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DefaultHandler2;

import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link DefaultDocumentLoader} that also records, on every element, the line its start tag
 * begins on. Spring's DOM parse is unchanged (same validation, same entity resolution); a
 * second, non-validating SAX pass over the same bytes collects the lines, which are then
 * attached to the DOM elements in document order as user data.
 *
 * <p>Read a line back with {@link #lineOf(Node)}; {@link XmlSourceExtractor} does that for
 * every bean definition. Line tracking is best effort: when the two passes disagree (which
 * they never should for well-formed input), or the SAX pass fails, the document is returned
 * without lines rather than failing the load.
 *
 * @author Max Arulananthan
 * @since 2.1
 */
public class LineTrackingDocumentLoader extends DefaultDocumentLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(LineTrackingDocumentLoader.class);

    /** DOM user-data key under which the line number ({@link Integer}) is stored. */
    public static final String LINE_NUMBER_KEY = "org.rulii.spring.xml.lineNumber";

    public LineTrackingDocumentLoader() {
        super();
    }

    /**
     * The line recorded for a node by this loader.
     *
     * @param node the node; may be null.
     * @return the 1-based line of the element's start tag, or null when not recorded.
     */
    public static Integer lineOf(Node node) {
        return node != null && node.getUserData(LINE_NUMBER_KEY) instanceof Integer line ? line : null;
    }

    @Override
    public Document loadDocument(InputSource inputSource, EntityResolver entityResolver, ErrorHandler errorHandler,
                                 int validationMode, boolean namespaceAware) throws Exception {
        Buffered content = Buffered.of(inputSource);

        Document document = super.loadDocument(content.inputSource(), entityResolver, errorHandler,
                validationMode, namespaceAware);

        try {
            annotateLines(document, content.inputSource(), entityResolver);
        } catch (Exception e) {
            LOGGER.debug("Line tracking skipped for [" + inputSource.getSystemId() + "]: " + e.getMessage(), e);
        }

        return document;
    }

    private void annotateLines(Document document, InputSource inputSource, EntityResolver entityResolver) throws Exception {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(false);

        XMLReader reader = factory.newSAXParser().getXMLReader();
        LineCollector collector = new LineCollector(entityResolver);
        reader.setContentHandler(collector);
        reader.setEntityResolver(collector);
        reader.setProperty("http://xml.org/sax/properties/lexical-handler", collector);
        reader.parse(inputSource);

        List<Element> elements = new ArrayList<>();
        collect(document.getDocumentElement(), elements);

        if (elements.size() != collector.lines.size()) {
            LOGGER.debug("Line tracking skipped for [" + inputSource.getSystemId() + "]: DOM has "
                    + elements.size() + " elements, SAX saw " + collector.lines.size());
            return;
        }

        for (int i = 0; i < elements.size(); i++) {
            elements.get(i).setUserData(LINE_NUMBER_KEY, collector.lines.get(i), null);
        }
    }

    /** Depth-first, document-order element traversal - the order SAX reports start tags in. */
    private static void collect(Element element, List<Element> into) {
        if (element == null) return;
        into.add(element);
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element child) collect(child, into);
        }
    }

    /**
     * Records the line each start tag begins on. A SAX {@link Locator} points just past the
     * event being reported, so the position at the end of the previous event (text, comment,
     * another tag) is where the current start tag's {@code <} sits.
     */
    private static final class LineCollector extends DefaultHandler2 {

        private final EntityResolver entityResolver;
        private final List<Integer> lines = new ArrayList<>();
        private Locator locator;
        private int lastLine = 1;

        LineCollector(EntityResolver entityResolver) {
            super();
            this.entityResolver = entityResolver;
        }

        @Override
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            lines.add(lastLine);
            mark();
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            mark();
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            mark();
        }

        @Override
        public void ignorableWhitespace(char[] ch, int start, int length) {
            mark();
        }

        @Override
        public void processingInstruction(String target, String data) {
            mark();
        }

        @Override
        public void comment(char[] ch, int start, int length) {
            mark();
        }

        @Override
        public void startCDATA() {
            mark();
        }

        @Override
        public void endCDATA() {
            mark();
        }

        @Override
        public void endDTD() {
            mark();
        }

        @Override
        public void startDocument() {
            mark();
        }

        private void mark() {
            if (locator != null && locator.getLineNumber() > 0) lastLine = locator.getLineNumber();
        }

        @Override
        public InputSource resolveEntity(String name, String publicId, String baseURI, String systemId)
                throws SAXException, IOException {
            return resolveEntity(publicId, systemId);
        }

        @Override
        public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
            if (entityResolver != null) {
                InputSource resolved = entityResolver.resolveEntity(publicId, systemId);
                if (resolved != null) return resolved;
            }
            // Never fetch DTDs over the network for the line pass.
            return new InputSource(new StringReader(""));
        }
    }

    /**
     * The input, read once and replayable: an {@link InputSource} can only be consumed once,
     * and the two passes both need it.
     */
    private record Buffered(InputSource original, byte[] bytes, String text) {

        static Buffered of(InputSource source) throws IOException {
            if (source.getCharacterStream() != null) {
                return new Buffered(source, null, FileCopyUtils.copyToString(source.getCharacterStream()));
            }
            if (source.getByteStream() != null) {
                return new Buffered(source, FileCopyUtils.copyToByteArray(source.getByteStream()), null);
            }
            // A system-id-only source: each pass re-opens it.
            return new Buffered(source, null, null);
        }

        InputSource inputSource() {
            InputSource copy = new InputSource();
            copy.setSystemId(original.getSystemId());
            copy.setPublicId(original.getPublicId());
            copy.setEncoding(original.getEncoding());
            if (text != null) copy.setCharacterStream(new StringReader(text));
            else if (bytes != null) copy.setByteStream(new ByteArrayInputStream(bytes));
            return copy;
        }
    }
}
