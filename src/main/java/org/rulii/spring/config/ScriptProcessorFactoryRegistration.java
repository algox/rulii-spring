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
package org.rulii.spring.config;

import org.rulii.script.ScriptProcessorFactory;
import org.rulii.script.ScriptProcessorManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Registers this application context's {@link ScriptProcessorFactory} beans with the
 * process-wide {@link ScriptProcessorManager} for the lifetime of the context, and takes
 * them back out when the context closes.
 *
 * <p>The manager is a JVM-wide singleton: a factory left registered after its context
 * closed keeps that context reachable through whatever the factory references (injected
 * collaborators, the bean class loader), which matters for test suites, DevTools restarts
 * and redeploys. Removal is by identity, so a factory a newer context has since replaced
 * under the same language name is left alone; names this context's factories had displaced
 * are refilled from service-loader discovery.
 *
 * @author Max Arulananthan
 * @since 2.1
 * @see ScriptTextResolverConfigurer
 */
public class ScriptProcessorFactoryRegistration implements InitializingBean, DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptProcessorFactoryRegistration.class);

    private final ObjectProvider<ScriptProcessorFactory> factories;
    private final List<ScriptProcessorFactory> registered = new ArrayList<>();

    /**
     * @param factories the context's ScriptProcessorFactory beans; must not be null.
     */
    public ScriptProcessorFactoryRegistration(ObjectProvider<ScriptProcessorFactory> factories) {
        super();
        Assert.notNull(factories, "factories cannot be null.");
        this.factories = factories;
    }

    @Override
    public void afterPropertiesSet() {
        ScriptProcessorManager manager = ScriptProcessorManager.getInstance();

        factories.orderedStream().forEach(factory -> {
            LOGGER.info("Registering custom ScriptProcessor [" + factory.getClass() + "] Language [" + factory.getLanguageName() + "]");
            // Force the manager's lazy ServiceLoader discovery to run BEFORE this explicit
            // registration. load() registers discovered defaults unconditionally, so
            // registering first would let the first rule evaluation silently clobber this
            // factory with the ServiceLoader default for the same language.
            manager.getScriptProcessorFactory(factory.getLanguageName());
            manager.register(factory);
            registered.add(factory);
        });
    }

    /**
     * The factories this registration put into the manager, in registration order.
     *
     * @return unmodifiable view; never null.
     */
    public List<ScriptProcessorFactory> getRegistered() {
        return Collections.unmodifiableList(registered);
    }

    @Override
    public void destroy() {
        ScriptProcessorManager manager = ScriptProcessorManager.getInstance();
        for (ScriptProcessorFactory factory : registered) manager.unregister(factory);
        registered.clear();
    }
}
