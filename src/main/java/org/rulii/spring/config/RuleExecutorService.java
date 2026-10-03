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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.Assert;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The default async rule-execution pool: a fixed-size {@link ThreadPoolExecutor} with a
 * bounded queue, caller-runs rejection, {@code rulii-exec-N} thread names, and a shutdown
 * tied to the application context.
 *
 * <p>{@link #destroy()} runs {@link #shutdown()}, waits up to the configured grace period
 * for running and queued rules to finish, and then {@link #shutdownNow() interrupts} what is
 * left. A plain {@code shutdown()} would let a stuck or long-running rule keep this pool's
 * non-daemon threads - and with them the context's class loader - alive indefinitely after
 * the context closed.
 *
 * @author Max Arulananthan
 * @since 2.1
 */
public class RuleExecutorService extends ThreadPoolExecutor implements DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuleExecutorService.class);

    private final long awaitTerminationSeconds;

    /**
     * @param threads                 the fixed pool size; must be positive
     * @param queueCapacity           the bounded queue capacity; must be positive
     * @param awaitTerminationSeconds how long {@link #destroy()} waits for in-flight work before
     *                                interrupting it; zero or negative interrupts immediately
     */
    public RuleExecutorService(int threads, int queueCapacity, long awaitTerminationSeconds) {
        super(threads, threads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(queueCapacity),
                new NamedThreadFactory(), new ThreadPoolExecutor.CallerRunsPolicy());
        Assert.isTrue(threads > 0, "threads must be positive.");
        Assert.isTrue(queueCapacity > 0, "queueCapacity must be positive.");
        this.awaitTerminationSeconds = awaitTerminationSeconds;
    }

    /**
     * The grace period {@link #destroy()} gives in-flight rules before interrupting them.
     *
     * @return the grace period in seconds
     */
    public long getAwaitTerminationSeconds() {
        return awaitTerminationSeconds;
    }

    @Override
    public void destroy() {
        shutdown();
        boolean interrupted = false;

        try {
            if (awaitTerminationSeconds > 0 && awaitTermination(awaitTerminationSeconds, TimeUnit.SECONDS)) return;

            int dropped = shutdownNow().size();
            LOGGER.warn("Rule executor did not finish within " + awaitTerminationSeconds
                    + "s; interrupting running rules and dropping " + dropped + " queued task(s).");

            if (!awaitTermination(1, TimeUnit.SECONDS)) {
                LOGGER.warn("Rule executor threads did not stop after being interrupted; check for rules ignoring interruption.");
            }
        } catch (InterruptedException e) {
            shutdownNow();
            interrupted = true;
        } finally {
            if (interrupted) Thread.currentThread().interrupt();
        }
    }

    /** Names threads {@code rulii-exec-N}; everything else matches the JDK default factory. */
    private static final class NamedThreadFactory implements ThreadFactory {

        private static final AtomicInteger POOL_NUMBER = new AtomicInteger(1);
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String prefix;

        NamedThreadFactory() {
            super();
            int pool = POOL_NUMBER.getAndIncrement();
            this.prefix = pool == 1 ? "rulii-exec-" : "rulii-exec-" + pool + "-";
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, prefix + threadNumber.getAndIncrement());
            if (t.isDaemon()) t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY) t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }
}
