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
import org.springframework.beans.factory.xml.PluggableSchemaResolver;
import org.xml.sax.InputSource;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies that META-INF/spring.schemas maps both URL variants of the rulii schema
 * location to the XSD bundled in the jar, so schema validation never goes to the network.
 *
 * <p>Regression test: only the {@code http://} URL was mapped, while all documentation
 * and example files declare the {@code https://} URL in {@code xsi:schemaLocation}.
 * Spring's {@link PluggableSchemaResolver} matches systemIds exactly, so every context
 * refresh silently fetched the XSD from www.rulii.org and failed on offline builds.
 *
 * @author Max Arulananthan
 * @since 2.0
 */
class SchemaMappingTest {

    private final PluggableSchemaResolver resolver =
            new PluggableSchemaResolver(SchemaMappingTest.class.getClassLoader());

    @Test
    void httpsSchemaUrlResolvesToBundledXsd() throws Exception {
        InputSource source = resolver.resolveEntity(null, "https://www.rulii.org/spring/rulii-spring.xsd");
        assertNotNull(source, "https schema URL must resolve to the XSD bundled in the jar");
    }

    @Test
    void httpSchemaUrlResolvesToBundledXsd() throws Exception {
        InputSource source = resolver.resolveEntity(null, "http://www.rulii.org/spring/rulii-spring.xsd");
        assertNotNull(source, "http schema URL must resolve to the XSD bundled in the jar");
    }
}
