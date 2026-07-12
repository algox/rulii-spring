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
package org.rulii.spring.test.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.rulii.context.RuleContext;
import org.rulii.context.RuleContextOptions;
import org.rulii.rule.Rule;
import org.rulii.rule.RuleListener;
import org.rulii.trace.RuliiListener;
import org.rulii.trace.Tracer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.atomic.AtomicInteger;

import static org.rulii.model.action.Actions.action;
import static org.rulii.model.condition.Conditions.condition;

/**
 * Verifies that the auto-configured Tracer bean registers RuliiListener and RuleListener
 * beans found in the application context, and that rule execution events reach them.
 *
 * @author Max Arulananthan
 * @since 2.0
 */
@SpringBootTest
public class TracerListenerRegistrationTest {

    @Autowired
    private Tracer tracer;
    @Autowired
    private RuleContextOptions ruleContextOptions;
    @Autowired
    private CountingRuliiListener ruliiListener;
    @Autowired
    private CountingRuleListener ruleListener;
    @Autowired
    private CountingDualListener dualListener;
    @Autowired
    private CountingRuleFlowListener ruleFlowListener;

    public TracerListenerRegistrationTest() {
        super();
    }

    @Test
    public void testTracerBeanIsWiredIntoContextOptions() {
        Assertions.assertNotNull(tracer);
        Assertions.assertEquals(tracer, ruleContextOptions.getTracer());
    }

    @Test
    public void testListenerBeansReceiveRuleEvents() {
        int ruliiStartsBefore = ruliiListener.getRuleStartCount();
        int ruleStartsBefore = ruleListener.getRuleStartCount();

        Rule rule = Rule.builder()
                .name("tracerProbeRule")
                .given(condition(() -> true))
                .then(action(() -> {}))
                .build();

        RuleContext context = RuleContext.builder().with(ruleContextOptions).build();
        rule.run(context);

        // Each listener bean must receive the event exactly once — a RuliiListener bean
        // must not be double-registered through the narrower RuleListener collection.
        Assertions.assertEquals(ruliiStartsBefore + 1, ruliiListener.getRuleStartCount());
        Assertions.assertEquals(ruleStartsBefore + 1, ruleListener.getRuleStartCount());
    }

    @Test
    public void testRuleFlowListenerBeanReceivesFlowEvents() {
        int startsBefore = ruleFlowListener.getFlowStartCount();

        org.rulii.ruleflow.RuleFlow<?> flow = org.rulii.ruleflow.RuleFlow.builder()
                .name("tracerProbeFlow")
                .bind("probe", 1)
                .build();

        flow.run(RuleContext.builder().with(ruleContextOptions).build());

        Assertions.assertEquals(startsBefore + 1, ruleFlowListener.getFlowStartCount(),
                "RuleFlowListener beans must be registered on the auto-configured Tracer");
    }

    @Test
    public void testListenerImplementingMultipleInterfacesRegistersOnce() {
        int startsBefore = dualListener.getRuleStartCount();

        Rule rule = Rule.builder()
                .name("dualProbeRule")
                .given(condition(() -> true))
                .then(action(() -> {}))
                .build();

        rule.run(RuleContext.builder().with(ruleContextOptions).build());

        // A bean implementing BOTH RuleListener and RuleSetListener appears in both
        // provider streams but must be registered (and receive events) exactly once.
        Assertions.assertEquals(startsBefore + 1, dualListener.getRuleStartCount());
    }

    @TestConfiguration
    static class ListenerConfig {

        public ListenerConfig() {
            super();
        }

        @Bean
        public CountingRuliiListener countingRuliiListener() {
            return new CountingRuliiListener();
        }

        @Bean
        public CountingRuleListener countingRuleListener() {
            return new CountingRuleListener();
        }

        @Bean
        public CountingDualListener countingDualListener() {
            return new CountingDualListener();
        }

        @Bean
        public CountingRuleFlowListener countingRuleFlowListener() {
            return new CountingRuleFlowListener();
        }
    }

    static class CountingRuliiListener implements RuliiListener {

        private final AtomicInteger ruleStartCount = new AtomicInteger();

        public CountingRuliiListener() {
            super();
        }

        @Override
        public void onRuleStart(Rule rule) {
            ruleStartCount.incrementAndGet();
        }

        public int getRuleStartCount() {
            return ruleStartCount.get();
        }
    }

    static class CountingRuleListener implements RuleListener {

        private final AtomicInteger ruleStartCount = new AtomicInteger();

        public CountingRuleListener() {
            super();
        }

        @Override
        public void onRuleStart(Rule rule) {
            ruleStartCount.incrementAndGet();
        }

        public int getRuleStartCount() {
            return ruleStartCount.get();
        }
    }

    static class CountingRuleFlowListener implements org.rulii.ruleflow.RuleFlowListener {

        private final AtomicInteger flowStartCount = new AtomicInteger();

        public CountingRuleFlowListener() {
            super();
        }

        @Override
        public void onRuleFlowStart(org.rulii.ruleflow.RuleFlow<?> ruleFlow, org.rulii.bind.NamedScope ruleFlowScope) {
            flowStartCount.incrementAndGet();
        }

        public int getFlowStartCount() {
            return flowStartCount.get();
        }
    }

    static class CountingDualListener implements RuleListener, org.rulii.ruleset.RuleSetListener {

        private final AtomicInteger ruleStartCount = new AtomicInteger();

        public CountingDualListener() {
            super();
        }

        @Override
        public void onRuleStart(Rule rule) {
            ruleStartCount.incrementAndGet();
        }

        public int getRuleStartCount() {
            return ruleStartCount.get();
        }
    }
}
