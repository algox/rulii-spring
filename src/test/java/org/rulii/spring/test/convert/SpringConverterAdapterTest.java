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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rulii.spring.convert.SpringConverterAdapter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SpringConverterAdapterTest {

    @Mock
    private ConversionService conversionService;

    @Test
    public void testGetSourceType() {
        SpringConverterAdapter adapter = new SpringConverterAdapter(conversionService);
        assertEquals(Object.class, adapter.getSourceType());
    }

    @Test
    public void testGetTargetType() {
        SpringConverterAdapter adapter = new SpringConverterAdapter(conversionService);
        assertEquals(Object.class, adapter.getTargetType());
    }

    @Test
    public void testCanConvertReturnsTrueWhenSupported() {
        when(conversionService.canConvert(any(TypeDescriptor.class), any(TypeDescriptor.class))).thenReturn(true);
        SpringConverterAdapter adapter = new SpringConverterAdapter(conversionService);
        assertTrue(adapter.canConvert(String.class, Integer.class));
    }

    @Test
    public void testCanConvertReturnsFalseWhenNotSupported() {
        when(conversionService.canConvert(any(TypeDescriptor.class), any(TypeDescriptor.class))).thenReturn(false);
        SpringConverterAdapter adapter = new SpringConverterAdapter(conversionService);
        assertFalse(adapter.canConvert(String.class, Object.class));
    }

    @Test
    public void testCanConvertDelegatesToConversionService() {
        when(conversionService.canConvert(any(TypeDescriptor.class), any(TypeDescriptor.class))).thenReturn(true);
        SpringConverterAdapter adapter = new SpringConverterAdapter(conversionService);
        adapter.canConvert(String.class, Double.class);
        verify(conversionService).canConvert(any(TypeDescriptor.class), any(TypeDescriptor.class));
    }

    @Test
    public void testConvertDelegatesToConversionService() {
        when(conversionService.convert(any(), any(TypeDescriptor.class), any(TypeDescriptor.class))).thenReturn(42);
        SpringConverterAdapter adapter = new SpringConverterAdapter(conversionService);
        Object result = adapter.convert("42", Integer.class);
        assertEquals(42, result);
    }

    @Test
    public void testConvertNullSourceReturnsNull() {
        SpringConverterAdapter adapter = new SpringConverterAdapter(conversionService);
        assertNull(adapter.convert(null, Integer.class));
        verifyNoInteractions(conversionService);
    }

    @Test
    public void testConvertCallsConversionServiceWithCorrectArgs() {
        when(conversionService.convert(any(), any(TypeDescriptor.class), any(TypeDescriptor.class))).thenReturn("result");
        SpringConverterAdapter adapter = new SpringConverterAdapter(conversionService);
        adapter.convert("source", String.class);
        verify(conversionService).convert(eq("source"), any(TypeDescriptor.class), any(TypeDescriptor.class));
    }

    @Test
    public void testNullConversionServiceThrows() {
        assertThrows(Exception.class, () -> new SpringConverterAdapter(null));
    }

    @Test
    public void testToString() {
        SpringConverterAdapter adapter = new SpringConverterAdapter(conversionService);
        String str = adapter.toString();
        assertNotNull(str);
        assertTrue(str.contains("SpringConverterAdapter"));
    }
}
