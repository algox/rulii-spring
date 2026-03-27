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
package org.rulii.spring.test.model;

import org.rulii.convert.ConversionException;
import org.rulii.convert.ConverterTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.lang.reflect.Type;

@Component
public class PersonConverter extends ConverterTemplate<String, Person> {

    public PersonConverter() {
        super();
    }

    @Override
    public Person convert(String value, Type toType) throws ConversionException {
        Assert.notNull(value, "value cannot be null.");
        String[] values = value.split(",");
        if (values.length != 3) return null;
        return new Person(values[0], values[1], Integer.parseInt(values[2]));
    }
}
