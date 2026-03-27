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
package org.rulii.spring.test.bind.load;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rulii.bind.Bindings;
import org.rulii.spring.bind.load.SpringContextBindingLoader;
import org.springframework.beans.factory.ListableBeanFactory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SpringContextBindingLoaderTest {

    @Mock
    private ListableBeanFactory beanFactory;

    @Test
    public void testLoadBindsSingleBean() {
        when(beanFactory.getBeanDefinitionNames()).thenReturn(new String[]{"myBean"});
        when(beanFactory.getBean("myBean")).thenReturn("beanValue");

        Bindings bindings = Bindings.builder().standard();
        SpringContextBindingLoader loader = new SpringContextBindingLoader();
        loader.load(bindings, beanFactory);

        assertTrue(bindings.contains("myBean"));
    }

    @Test
    public void testLoadBindingHasCorrectValue() {
        when(beanFactory.getBeanDefinitionNames()).thenReturn(new String[]{"myBean"});
        when(beanFactory.getBean("myBean")).thenReturn("beanValue");

        Bindings bindings = Bindings.builder().standard();
        SpringContextBindingLoader loader = new SpringContextBindingLoader();
        loader.load(bindings, beanFactory);

        assertEquals("beanValue", bindings.getValue("myBean"));
    }

    @Test
    public void testLoadBindingsAreReadOnly() {
        when(beanFactory.getBeanDefinitionNames()).thenReturn(new String[]{"readOnlyBean"});
        when(beanFactory.getBean("readOnlyBean")).thenReturn("value");

        Bindings bindings = Bindings.builder().standard();
        SpringContextBindingLoader loader = new SpringContextBindingLoader();
        loader.load(bindings, beanFactory);

        assertFalse(bindings.getBinding("readOnlyBean").isEditable());
    }

    @Test
    public void testLoadMultipleBeans() {
        when(beanFactory.getBeanDefinitionNames()).thenReturn(new String[]{"bean1", "bean2", "bean3"});
        when(beanFactory.getBean("bean1")).thenReturn("v1");
        when(beanFactory.getBean("bean2")).thenReturn(42);
        when(beanFactory.getBean("bean3")).thenReturn(true);

        Bindings bindings = Bindings.builder().standard();
        SpringContextBindingLoader loader = new SpringContextBindingLoader();
        loader.load(bindings, beanFactory);

        assertTrue(bindings.contains("bean1"));
        assertTrue(bindings.contains("bean2"));
        assertTrue(bindings.contains("bean3"));
    }

    @Test
    public void testLoadEmptyFactory() {
        when(beanFactory.getBeanDefinitionNames()).thenReturn(new String[0]);

        Bindings bindings = Bindings.builder().standard();
        SpringContextBindingLoader loader = new SpringContextBindingLoader();
        loader.load(bindings, beanFactory);

        assertEquals(0, bindings.size());
    }

    @Test
    public void testLoadNullBindingsThrows() {
        SpringContextBindingLoader loader = new SpringContextBindingLoader();
        assertThrows(Exception.class, () -> loader.load(null, beanFactory));
    }

    @Test
    public void testLoadNullFactoryThrows() {
        SpringContextBindingLoader loader = new SpringContextBindingLoader();
        assertThrows(Exception.class, () -> loader.load(Bindings.builder().standard(), null));
    }

    @Test
    public void testLoadBindingHasCorrectType() {
        when(beanFactory.getBeanDefinitionNames()).thenReturn(new String[]{"intBean"});
        when(beanFactory.getBean("intBean")).thenReturn(Integer.valueOf(100));

        Bindings bindings = Bindings.builder().standard();
        SpringContextBindingLoader loader = new SpringContextBindingLoader();
        loader.load(bindings, beanFactory);

        assertEquals(Integer.class, bindings.getBinding("intBean").getType());
    }
}
