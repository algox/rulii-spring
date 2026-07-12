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
import org.junit.jupiter.api.extension.ExtendWith;
import org.rulii.context.RuleContext;
import org.rulii.context.RuleContextOptions;
import org.rulii.registry.RuleRegistry;
import org.rulii.ruleflow.RuleFlow;
import org.rulii.spring.annotation.RuleScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * End-to-end tests for the async grammar of XML-declared RuleFlows: async-run by bean-ref
 * (sub-flows) with context-mode and then-run continuation, await, and await-all — executed
 * on the Spring-configured executor via the auto-configured {@link RuleContextOptions}.
 *
 * @author Max Arulananthan
 * @since 2.0
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = RuleFlowAsyncXmlTest.Config.class)
class RuleFlowAsyncXmlTest {

    // Plain @Configuration (not @SpringBootConfiguration) so other tests in this package
    // using default configuration resolution do not pick this class up.
    @Configuration
    @EnableAutoConfiguration
    @RuleScan(xmlLocations = "classpath:rules/ruleflow/")
    static class Config {}

    @Autowired
    private RuleRegistry ruleRegistry;

    @Autowired
    private RuleContextOptions options;

    @Test
    void asyncFanOutAwaitsAndCombinesResults() {
        RuleFlow<?> flow = ruleRegistry.getRuleFlow("AsyncFlow");
        assertNotNull(flow);

        RuleContext ctx = RuleContext.builder().with(options).build();
        ctx.getBindings().bind("v", 7);
        ctx.getBindings().bind("continued", 0);

        Object result = flow.run(ctx);

        // Two async SquareFlow runs (7*7 each) awaited and combined: 49 + 49.
        assertEquals(98, result);
        // The then-run continuation received f2's result and wrote it through.
        assertEquals(49, (Integer) ctx.getBindings().getValue("continued"));
    }
}
