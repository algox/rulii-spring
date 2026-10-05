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

import org.rulii.model.SourceDefinition;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.core.io.Resource;

/**
 * Turns a factory bean's own bean definition into the {@link SourceDefinition} its artifact
 * reports: the XML file and line when the definition carries an {@link XmlSource}, else the
 * defining resource alone, else nothing (and the builder falls back to its default).
 *
 * @author Max Arulananthan
 * @since 2.1
 */
final class BeanSources {

    private BeanSources() {
        super();
    }

    /**
     * The source of the named bean's definition.
     *
     * @param beanFactory the factory the bean lives in; may be null.
     * @param beanName    the bean's name; may be null.
     * @return a file source, or null when nothing is known.
     */
    static SourceDefinition sourceOf(BeanFactory beanFactory, String beanName) {
        if (beanName == null || !(beanFactory instanceof ConfigurableListableBeanFactory factory)) return null;
        if (!factory.containsBeanDefinition(beanName)) return null;

        BeanDefinition definition = factory.getBeanDefinition(beanName);

        if (definition.getSource() instanceof XmlSource xml) {
            return SourceDefinition.forFile(xml.location(), xml.line());
        }

        Resource resource = definition instanceof AbstractBeanDefinition abstractDefinition ? abstractDefinition.getResource() : null;
        if (resource != null) return SourceDefinition.forFile(XmlSource.location(resource), null);

        String description = definition.getResourceDescription();
        return description != null && !description.isBlank() ? SourceDefinition.forFile(description, null) : null;
    }
}
