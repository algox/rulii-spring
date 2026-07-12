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
package org.rulii.spring.test.convert;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.rulii.convert.ConversionException;
import org.rulii.convert.Converter;
import org.rulii.convert.ConverterRegistry;
import org.rulii.spring.convert.SpringConverterAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies converter precedence: application-registered Spring converters win over
 * rulii's built-in defaults, and Spring conversion failures surface as rulii's
 * {@link ConversionException}.
 *
 * <p>Regression test: the Spring ConversionService bridge was previously registered
 * AFTER rulii's defaults, and rulii's converter lookup is first-match — so a custom
 * Spring converter for a pair the defaults covered (e.g. String→LocalDate with a
 * non-ISO format) was silently never consulted.
 *
 * @author Max Arulananthan
 * @since 2.0
 */
@ExtendWith(org.springframework.test.context.junit.jupiter.SpringExtension.class)
@SpringBootTest(classes = ConverterPrecedenceTest.Config.class)
class ConverterPrecedenceTest {

    private static final DateTimeFormatter CUSTOM_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Plain @Configuration (not @SpringBootConfiguration) so other tests in this package
    // using default configuration resolution do not pick this class up.
    @Configuration
    @EnableAutoConfiguration
    static class Config {

        @Bean
        public ConversionService conversionService() {
            DefaultConversionService service = new DefaultConversionService();
            service.addConverter(String.class, LocalDate.class, text -> LocalDate.parse(text, CUSTOM_FORMAT));
            return service;
        }
    }

    @Autowired
    private ConverterRegistry converterRegistry;

    @Test
    void applicationSpringConverterWinsOverRuliiDefault() {
        Converter<String, LocalDate> converter = converterRegistry.find(String.class, LocalDate.class);

        assertNotNull(converter);
        assertEquals(SpringConverterAdapter.class, converter.getClass(),
                "the Spring ConversionService bridge must take precedence over rulii's built-in defaults");
        assertEquals(LocalDate.of(2026, 12, 25), converter.convert("25/12/2026", LocalDate.class),
                "the custom dd/MM/yyyy Spring converter must be the one consulted");
    }

    @Test
    void springConversionFailureSurfacesAsRuliiConversionException() {
        Converter<String, Integer> converter = converterRegistry.find(String.class, Integer.class);
        assertNotNull(converter);

        ConversionException ex = assertThrows(ConversionException.class,
                () -> converter.convert("not-a-number", Integer.class));
        assertNotNull(ex.getCause(), "the Spring cause must be preserved");
    }
}
