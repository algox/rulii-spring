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

import org.junit.jupiter.api.Test;
import org.rulii.script.ScriptCompiler;
import org.rulii.script.ScriptOptions;
import org.rulii.script.ScriptProcessor;
import org.rulii.spring.script.el.SpelScriptCompiler;
import org.rulii.spring.script.el.SpelScriptProcessor;
import org.rulii.spring.script.el.SpelScriptProcessorFactory;

import static org.junit.jupiter.api.Assertions.*;

public class SpelScriptProcessorFactoryTest {

    @Test
    public void testLanguageNameConstant() {
        assertEquals("el", SpelScriptProcessorFactory.LANGUAGE_NAME);
    }

    @Test
    public void testDefaultLanguageName() {
        SpelScriptProcessorFactory factory = new SpelScriptProcessorFactory();
        assertEquals("el", factory.getLanguageName());
    }

    @Test
    public void testDefaultBindingName() {
        SpelScriptProcessorFactory factory = new SpelScriptProcessorFactory();
        assertEquals(ScriptOptions.DEFAULT.bindingsName(), factory.getBindingsName());
    }

    @Test
    public void testCustomLanguageName() {
        SpelScriptProcessorFactory factory = new SpelScriptProcessorFactory("myLang", "myCtx");
        assertEquals("myLang", factory.getLanguageName());
    }

    @Test
    public void testCustomBindingName() {
        SpelScriptProcessorFactory factory = new SpelScriptProcessorFactory("el", "myCtx");
        assertEquals("myCtx", factory.getBindingsName());
    }

    @Test
    public void testGetScriptProcessorReturnsNonNull() {
        SpelScriptProcessorFactory factory = new SpelScriptProcessorFactory();
        ScriptProcessor processor = factory.getScriptProcessor();
        assertNotNull(processor);
    }

    @Test
    public void testGetScriptProcessorReturnsSpelProcessor() {
        SpelScriptProcessorFactory factory = new SpelScriptProcessorFactory();
        ScriptProcessor processor = factory.getScriptProcessor();
        assertInstanceOf(SpelScriptProcessor.class, processor);
    }

    @Test
    public void testGetScriptProcessorPropagatesLanguageName() {
        SpelScriptProcessorFactory factory = new SpelScriptProcessorFactory("el", "ctx");
        ScriptProcessor processor = factory.getScriptProcessor();
        assertEquals("el", processor.getLanguageName());
    }

    @Test
    public void testGetScriptProcessorPropagatesBindingName() {
        SpelScriptProcessorFactory factory = new SpelScriptProcessorFactory("el", "myCtx");
        ScriptProcessor processor = factory.getScriptProcessor();
        assertEquals("myCtx", processor.getBindingsName());
    }

    @Test
    public void testGetScriptCompilerReturnsNonNull() {
        SpelScriptProcessorFactory factory = new SpelScriptProcessorFactory();
        ScriptCompiler compiler = factory.getScriptCompiler();
        assertNotNull(compiler);
    }

    @Test
    public void testGetScriptCompilerReturnsSpelCompiler() {
        SpelScriptProcessorFactory factory = new SpelScriptProcessorFactory();
        ScriptCompiler compiler = factory.getScriptCompiler();
        assertInstanceOf(SpelScriptCompiler.class, compiler);
    }

    @Test
    public void testGetScriptProcessorCreatesNewInstanceEachCall() {
        SpelScriptProcessorFactory factory = new SpelScriptProcessorFactory();
        ScriptProcessor p1 = factory.getScriptProcessor();
        ScriptProcessor p2 = factory.getScriptProcessor();
        assertNotSame(p1, p2);
    }

    @Test
    public void testGetScriptCompilerCreatesNewInstanceEachCall() {
        SpelScriptProcessorFactory factory = new SpelScriptProcessorFactory();
        ScriptCompiler c1 = factory.getScriptCompiler();
        ScriptCompiler c2 = factory.getScriptCompiler();
        assertNotSame(c1, c2);
    }

    @Test
    public void testNullLanguageNameThrows() {
        assertThrows(Exception.class, () -> new SpelScriptProcessorFactory(null, "ctx"));
    }

    @Test
    public void testEmptyLanguageNameThrows() {
        assertThrows(Exception.class, () -> new SpelScriptProcessorFactory("", "ctx"));
    }

    @Test
    public void testNullBindingNameThrows() {
        assertThrows(Exception.class, () -> new SpelScriptProcessorFactory("el", null));
    }

    @Test
    public void testEmptyBindingNameThrows() {
        assertThrows(Exception.class, () -> new SpelScriptProcessorFactory("el", ""));
    }
}
