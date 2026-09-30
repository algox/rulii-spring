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

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Where a rulii bean definition came from: the XML file and, when known, the line of the
 * element that declared it.
 *
 * <p>Set as the {@link org.springframework.beans.factory.config.BeanDefinition#getSource() source}
 * of every bean definition the rulii parsers register (top-level and inline) when the XML is
 * read with an {@link XmlSourceExtractor}. {@code @RuleScan(xmlLocations)} wires that up,
 * together with the {@link LineTrackingDocumentLoader} that supplies the lines; XML loaded
 * another way ({@code @ImportResource}, {@code <import>}) keeps Spring's default: the resource
 * description on the bean definition, no source.
 *
 * <p>The rulii factory beans turn this into the artifact's
 * {@link org.rulii.model.SourceDefinition}, so {@code getDefinition().getSource()} names the
 * file and line for XML-built rules, rule sets and rule flows.
 *
 * @param resource the XML file; may be null when the reader had no defining resource.
 * @param location the file as the application knows it, stable across machines:
 *                 {@code classpath:rules/order.xml} rather than an absolute path; never null.
 * @param line     1-based line of the element's start tag; null when the document was not
 *                 loaded with line tracking.
 *
 * @author Max Arulananthan
 * @since 2.1
 */
public record XmlSource(Resource resource, String location, Integer line) {

    public XmlSource {
        Assert.hasText(location, "location cannot be empty/null.");
    }

    /**
     * Creates a source whose location is derived from the resource with {@link #location(Resource)}.
     *
     * @param resource the XML file; may be null.
     * @param line     the line; may be null.
     */
    public XmlSource(Resource resource, Integer line) {
        this(resource, location(resource), line);
    }

    /**
     * The default location label for a resource: {@code classpath:} plus the path for a
     * {@link ClassPathResource}, else the resource description.
     *
     * @param resource the resource; may be null.
     * @return a non-empty label.
     */
    public static String location(Resource resource) {
        if (resource == null) return "unknown resource";
        if (resource instanceof ClassPathResource classPath) return "classpath:" + classPath.getPath();
        return resource.getDescription();
    }

    /**
     * The location followed by {@code :line} when the line is known.
     *
     * @return a human-readable location; never null.
     */
    public String describe() {
        return line != null ? location + ":" + line : location;
    }

    @Override
    public String toString() {
        return "XmlSource{" + describe() + "}";
    }
}
