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
import org.rulii.annotation.Rule;
import org.rulii.annotation.Then;
import org.rulii.spring.test.model.Person;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

@Rule(name = "testRule1")
public class TestRule1 {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestRule1.class);

    private final Person person;

    public TestRule1(Person person) {
        super();
        Assert.notNull(person, "person cannot be null.");
        this.person = person;
    }

    @Given
    public boolean when(String arg1, String arg2) {
        return true;
    }

    @Then
    public void then() {
        LOGGER.info("XXX TestRule1 [" + person + "]");
    }

    @Then
    public void then(Integer x) {}

    public Person getPerson() {
        return person;
    }
}
