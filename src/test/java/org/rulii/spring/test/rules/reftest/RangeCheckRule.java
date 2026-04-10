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
package org.rulii.spring.test.rules.reftest;

import org.rulii.annotation.Given;

/**
 * Test rule that passes when {@code value} is within [min, max].
 * Used to verify {@code <class-ref>} with typed constructor args.
 *
 * <p>Intentionally not annotated with {@code @Rule} to prevent {@code @RuleScan}
 * from picking it up as a Spring-managed bean during Spring Boot tests.
 */
public class RangeCheckRule {

    private final int min;
    private final int max;

    public RangeCheckRule(int min, int max) {
        this.min = min;
        this.max = max;
    }

    @Given
    public boolean when(int value) {
        return value >= min && value <= max;
    }

    public int getMin() { return min; }
    public int getMax() { return max; }
}
