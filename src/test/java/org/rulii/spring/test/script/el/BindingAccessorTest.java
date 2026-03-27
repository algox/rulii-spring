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
package org.rulii.spring.test.script.el;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rulii.bind.Bindings;
import org.rulii.spring.script.el.BindingAccessor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import static org.junit.jupiter.api.Assertions.*;

public class BindingAccessorTest {

    private BindingAccessor accessor;
    private EvaluationContext evalContext;

    @BeforeEach
    public void setUp() {
        accessor = new BindingAccessor();
        evalContext = new StandardEvaluationContext();
    }

    @Test
    public void testGetSpecificTargetClassesContainsBindings() {
        Class<?>[] classes = accessor.getSpecificTargetClasses();
        assertNotNull(classes);
        assertEquals(1, classes.length);
        assertEquals(Bindings.class, classes[0]);
    }

    @Test
    public void testCanReadReturnsTrueForExistingBinding() throws AccessException {
        Bindings bindings = Bindings.builder().standard();
        bindings.bind("age", 25);
        assertTrue(accessor.canRead(evalContext, bindings, "age"));
    }

    @Test
    public void testCanReadReturnsFalseForMissingBinding() throws AccessException {
        Bindings bindings = Bindings.builder().standard();
        assertFalse(accessor.canRead(evalContext, bindings, "notExists"));
    }

    @Test
    public void testCanReadReturnsFalseForNonBindingsTarget() throws AccessException {
        assertFalse(accessor.canRead(evalContext, "notABindings", "name"));
    }

    @Test
    public void testCanReadReturnsFalseForNullTarget() throws AccessException {
        assertFalse(accessor.canRead(evalContext, null, "name"));
    }

    @Test
    public void testReadReturnsCorrectStringValue() throws AccessException {
        Bindings bindings = Bindings.builder().standard();
        bindings.bind("name", "Alice");
        TypedValue value = accessor.read(evalContext, bindings, "name");
        assertNotNull(value);
        assertEquals("Alice", value.getValue());
    }

    @Test
    public void testReadReturnsCorrectIntValue() throws AccessException {
        Bindings bindings = Bindings.builder().standard();
        bindings.bind("count", 42);
        TypedValue value = accessor.read(evalContext, bindings, "count");
        assertEquals(42, value.getValue());
    }

    @Test
    public void testReadReturnsTypedValueNullForNonBindingsTarget() throws AccessException {
        TypedValue value = accessor.read(evalContext, "notABindings", "name");
        assertEquals(TypedValue.NULL, value);
    }

    @Test
    public void testReadNullBindingReturnsTypedValueNull() throws AccessException {
        Bindings bindings = Bindings.builder().standard();
        TypedValue value = accessor.read(evalContext, bindings, "nonExistentBinding");
        assertEquals(TypedValue.NULL, value);
    }

    @Test
    public void testCanWriteReturnsTrueForEditableBinding() throws AccessException {
        Bindings bindings = Bindings.builder().standard();
        bindings.bind("score", 10);
        assertTrue(accessor.canWrite(evalContext, bindings, "score"));
    }

    @Test
    public void testCanWriteReturnsTrueForNewBinding() throws AccessException {
        Bindings bindings = Bindings.builder().standard();
        assertTrue(accessor.canWrite(evalContext, bindings, "brandNewBinding"));
    }

    @Test
    public void testCanWriteReturnsFalseForNonBindingsTarget() throws AccessException {
        assertFalse(accessor.canWrite(evalContext, "notBindings", "name"));
    }

    @Test
    public void testWriteUpdatesExistingBinding() throws AccessException {
        Bindings bindings = Bindings.builder().standard();
        bindings.bind("score", 10);
        accessor.write(evalContext, bindings, "score", 99);
        assertEquals(99, (Integer) bindings.getValue("score"));
    }

    @Test
    public void testWriteCreatesNewBinding() throws AccessException {
        Bindings bindings = Bindings.builder().standard();
        accessor.write(evalContext, bindings, "newKey", "newValue");
        assertTrue(bindings.contains("newKey"));
        assertEquals("newValue", bindings.getValue("newKey"));
    }

    @Test
    public void testWriteToNonBindingsTargetDoesNotThrow() {
        assertDoesNotThrow(() -> accessor.write(evalContext, "notBindings", "key", "value"));
    }
}
