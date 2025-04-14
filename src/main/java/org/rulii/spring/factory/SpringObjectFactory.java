/*
 * This software is licensed under the Apache 2 license, quoted below.
 *
 * Copyright (c) 1999-2025, Algorithmx Inc.
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
package org.rulii.spring.factory;

import org.rulii.model.UnrulyException;
import org.rulii.util.reflect.DefaultObjectFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;

public class SpringObjectFactory extends DefaultObjectFactory {

    // Underlying Spring Factory that does the real work.
    private ListableBeanFactory ctx;

    public SpringObjectFactory(ListableBeanFactory ctx) {
        super(false);
        this.ctx = ctx;
    }

    public <T> T createRule(Class<T> ruleClass) throws UnrulyException {

        if (ctx instanceof AutowireCapableBeanFactory autowireCapableBeanFactory) {
            return autowireCapableBeanFactory.createBean(ruleClass);
        }

        return super.createRule(ruleClass);
    }

    @EventListener
    public void handleContextRefreshEvent(ContextClosedEvent ctxClosedEvent) {
        this.ctx = null;
    }

    private ListableBeanFactory getCtx() {
        if (ctx == null) throw new UnrulyException("Application Context is closed.");
        return ctx;
    }

    @Override
    public String toString() {
        return "SpringObjectFactory{" +
                "ctx=" + ctx +
                '}';
    }
}
