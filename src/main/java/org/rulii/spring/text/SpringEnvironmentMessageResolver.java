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
package org.rulii.spring.text;

import org.rulii.text.MessageResolver;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

import java.util.Locale;

/**
 * Resolves rule messages via Spring's standard i18n mechanism with an Environment fallback.
 *
 * <p>Resolution order:
 * <ol>
 *   <li>{@link MessageSource} (when one is available) — locale-aware, backed by the
 *       application's {@code messages*.properties} bundles</li>
 *   <li>{@link Environment} properties (e.g. {@code application.yaml}) — locale-insensitive</li>
 *   <li>the supplied default message</li>
 * </ol>
 *
 * @author Max Arulananthan
 * @since 1.0
 */
public class SpringEnvironmentMessageResolver implements MessageResolver {

    private final Environment environment;
    private final MessageSource messageSource;

    /**
     * Constructs a resolver backed only by the Environment (no MessageSource).
     *
     * @param environment the Spring Environment; must not be null
     */
    public SpringEnvironmentMessageResolver(Environment environment) {
        this(environment, null);
    }

    /**
     * Constructs a resolver that consults the given MessageSource first and falls back
     * to Environment properties.
     *
     * @param environment   the Spring Environment; must not be null
     * @param messageSource the MessageSource for locale-aware resolution; may be null
     */
    public SpringEnvironmentMessageResolver(Environment environment, MessageSource messageSource) {
        super();
        Assert.notNull(environment, "environment cannot be null.");
        this.environment = environment;
        this.messageSource = messageSource;
    }

    /**
     * Resolves a message for the given code: MessageSource first (locale-aware), then
     * Environment properties, then the default message.
     *
     * @param locale the Locale for which the message should be resolved; falls back to the
     *               JVM default when null (only relevant for MessageSource resolution)
     * @param code the code identifying the message to be resolved; a null code resolves
     *             to the default message
     * @param defaultMessage the default message to be returned if the code is not found
     * @return the resolved message for the given code, or the default message if not found
     */
    @Override
    public String resolve(Locale locale, String code, String defaultMessage) {
        if (code == null) return defaultMessage;

        if (messageSource != null) {
            String result = messageSource.getMessage(code, null, null,
                    locale != null ? locale : Locale.getDefault());
            if (result != null) return result;
        }

        try {
            return environment.getProperty(code, defaultMessage);
        } catch (IllegalArgumentException e) {
            // The property value contains an unresolvable ${...} placeholder - fall back
            // rather than failing message resolution for the whole violation.
            return defaultMessage;
        }
    }
}
