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
package org.rulii.spring.test.rules.seta;

import org.rulii.annotation.Given;
import org.rulii.annotation.Otherwise;
import org.rulii.annotation.Rule;
import org.springframework.beans.factory.annotation.Qualifier;

@Rule
public class TestRule3 {

    public TestRule3(@Qualifier("testRule2") org.rulii.rule.Rule rule2) {
        super();
    }

    @Given
    public boolean when() {
        return false;
    }

    @Otherwise
    public void otherwise() {
        System.err.println("XXX TestRule3");
    }
}
