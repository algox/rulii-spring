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

import org.junit.jupiter.api.Test;
import org.rulii.spring.config.BeanNames;
import org.rulii.spring.config.RuleExecutorService;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The default async pool must not outlive its context: a stuck rule is interrupted after
 * the grace period instead of pinning non-daemon threads (and the class loader) forever.
 */
class RuleExecutorServiceTest {

    // Plain @Configuration (not @SpringBootConfiguration) so other tests in this package
    // using default configuration resolution do not pick this class up.
    @Configuration
    @EnableAutoConfiguration
    static class Config {}

    @Test
    void destroyInterruptsStuckTaskAfterGracePeriod() throws Exception {
        RuleExecutorService executor = new RuleExecutorService(1, 10, 1);
        CountDownLatch started = new CountDownLatch(1);
        AtomicBoolean interrupted = new AtomicBoolean();
        AtomicReference<String> threadName = new AtomicReference<>();

        executor.execute(() -> {
            threadName.set(Thread.currentThread().getName());
            started.countDown();
            try {
                Thread.sleep(TimeUnit.MINUTES.toMillis(5));
            } catch (InterruptedException e) {
                interrupted.set(true);
            }
        });
        assertTrue(started.await(5, TimeUnit.SECONDS));

        long begin = System.nanoTime();
        executor.destroy();

        assertTrue(executor.isTerminated(), "destroy must not return with the pool still running");
        assertTrue(interrupted.get(), "the stuck task must have been interrupted");
        assertTrue(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - begin) < 30, "destroy must honour the grace period, not hang");
        assertTrue(threadName.get().startsWith("rulii-exec-"), "threads must be identifiable: " + threadName.get());
    }

    @Test
    void destroyLetsFinishingWorkCompleteWithinGracePeriod() throws Exception {
        RuleExecutorService executor = new RuleExecutorService(1, 10, 5);
        AtomicBoolean completed = new AtomicBoolean();

        executor.execute(() -> {
            try {
                Thread.sleep(200);
                completed.set(true);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        executor.destroy();

        assertTrue(executor.isTerminated());
        assertTrue(completed.get(), "work that finishes inside the grace period must not be interrupted");
    }

    @Test
    void autoConfiguredPoolHonoursGracePeriodPropertyAndStopsOnClose() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("executor-test",
                Map.of("rulii.executor.awaitTerminationSeconds", "3")));
        context.register(Config.class);
        context.refresh();

        ExecutorService executor = context.getBean(BeanNames.EXECUTOR_SERVICE, ExecutorService.class);
        RuleExecutorService pool = assertInstanceOf(RuleExecutorService.class, executor);
        assertEquals(3, pool.getAwaitTerminationSeconds());

        context.close();
        assertTrue(executor.isTerminated(), "closing the context must stop the pool");
    }
}
