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

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end tests for XML-declared RuleFlows ({@code <r:ruleflow>}).
 *
 * <p>Covers the synchronous grammar: params, literal/script binds, run by registry name
 * (execution-time lookup against the Spring registry) and by bean-ref (wiring-time),
 * when/then/otherwise with exit, for-each, step-scoped with-params, scope, step-level
 * exception handling, ${placeholder} defaults in expressions, finalizer, and returning.
 *
 * @author Max Arulananthan
 * @since 2.0
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = RuleFlowXmlTest.Config.class)
class RuleFlowXmlTest {

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

    /**
     * Flow-internal bindings live in the flow's own scope, which is removed after
     * execution — so every observable output slot is pre-bound here; the flow writes
     * through to them via {@code #ctx.x = ...} (setValueOrBind semantics).
     */
    private RuleContext ctx(int total) {
        RuleContext ctx = RuleContext.builder().with(options).build();
        ctx.getBindings().bind("total", total);
        ctx.getBindings().bind("sum", 0);
        ctx.getBindings().bind("ranViaRegistry", false);
        ctx.getBindings().bind("discountApplied", false);
        ctx.getBindings().bind("recovered", false);
        ctx.getBindings().bind("finalized", false);
        ctx.getBindings().bind("globallyHandled", false);
        ctx.getBindings().bind("snapshot", "");
        return ctx;
    }

    @Test
    void flowsAreDiscoverableViaRegistry() {
        assertNotNull(ruleRegistry.getRuleFlow("OrderFlow"));
        assertTrue(ruleRegistry.getRuleFlows().size() >= 3,
                "OrderFlow, SquareFlow, and AsyncFlow must all be enumerable");
    }

    @Test
    void approvedPathExercisesTheFullGrammar() {
        RuleFlow<?> flow = ruleRegistry.getRuleFlow("OrderFlow");
        RuleContext ctx = ctx(150);

        Object result = flow.run(ctx);

        assertEquals("approved", result, "returning must extract #ctx.status");
        assertEquals(Boolean.TRUE, ctx.getBindings().getValue("ranViaRegistry"), "run by registry name must execute the Spring rule");
        assertEquals(Boolean.TRUE, ctx.getBindings().getValue("discountApplied"), "run by bean-ref must execute the Spring rule bean");
        assertEquals(6, (Integer) ctx.getBindings().getValue("sum"), "for-each must iterate the SpEL list source");
        assertEquals(Boolean.TRUE, ctx.getBindings().getValue("recovered"), "step-level on-exception must handle the failing script");
        // literal bind, script bind, then-branch apply, and with-params - exported by the flow
        assertEquals("start|300|301|premium", ctx.getBindings().getValue("snapshot"));
        assertEquals(Boolean.TRUE, ctx.getBindings().getValue("finalized"), "finalizer must run");
        assertEquals(Boolean.FALSE, ctx.getBindings().getValue("globallyHandled"), "the global handler must not fire on the happy path");
    }

    @Test
    void rejectedPathExitsEarly() {
        RuleFlow<?> flow = ruleRegistry.getRuleFlow("OrderFlow");
        RuleContext ctx = ctx(50);   // below the ${order.flowMin:100} placeholder default

        flow.run(ctx);

        assertEquals(0, (Integer) ctx.getBindings().getValue("sum"), "exit must skip the for-each");
        assertEquals(Boolean.FALSE, ctx.getBindings().getValue("recovered"), "exit must skip subsequent commands");
        assertEquals("", ctx.getBindings().getValue("snapshot"), "exit must skip the snapshot export");
        assertEquals(Boolean.FALSE, ctx.getBindings().getValue("ranViaRegistry"), "the then-branch must not have run");
        assertEquals(Boolean.TRUE, ctx.getBindings().getValue("finalized"), "finalizer must run even on early exit");
    }
}
