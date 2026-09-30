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

import org.springframework.beans.factory.parsing.SourceExtractor;
import org.springframework.core.io.Resource;
import org.w3c.dom.Node;

/**
 * {@link SourceExtractor} that turns a DOM element into an {@link XmlSource}: the defining
 * resource plus the element's line, when the document was loaded by a
 * {@link LineTrackingDocumentLoader}.
 *
 * <p>Install on an {@link org.springframework.beans.factory.xml.XmlBeanDefinitionReader} with
 * {@code setSourceExtractor}; the rulii parsers then stamp every bean definition they register.
 * Spring's own {@code <bean>} definitions parsed by the same reader get an {@link XmlSource}
 * too.
 *
 * @author Max Arulananthan
 * @since 2.1
 */
public class XmlSourceExtractor implements SourceExtractor {

    public XmlSourceExtractor() {
        super();
    }

    @Override
    public Object extractSource(Object sourceCandidate, Resource definingResource) {
        if (!(sourceCandidate instanceof Node node)) return null;
        return new XmlSource(definingResource, LineTrackingDocumentLoader.lineOf(node));
    }
}
