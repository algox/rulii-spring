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
package org.rulii.spring.text;

import org.rulii.text.MessageResolver;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

import java.util.Locale;

/**
 * Manages resolving messages based on the Spring Environment.
 *
 * @author Max Arulananthan
 * @since 1.0
 */
public class SpringEnvironmentMessageResolver implements MessageResolver {

    private final Environment environment;
    public SpringEnvironmentMessageResolver(Environment environment) {
        super();
        Assert.notNull(environment, "environment cannot be null.");
        this.environment = environment;
    }

    /**
     * Resolves a message based on the given message code, and default message. The locale is ignored as the resolution
     * is based on the Spring Environment.
     *
     * @param locale the Locale for which the message should be resolved. This value is ignored.
     * @param code the code identifying the message to be resolved
     * @param defaultMessage the default message to be returned if the code is not found
     * @return the resolved message for the given code, or the default message if not found
     */
    @Override
    public String resolve(Locale locale, String code, String defaultMessage) {
        return environment.getProperty(code, defaultMessage);
    }
}
