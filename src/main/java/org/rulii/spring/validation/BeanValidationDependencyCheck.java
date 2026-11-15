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
package org.rulii.spring.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Says so when {@code rulii.validation.enabled=true} is set but {@code rulii-bean-validation} is not on the
 * classpath: the dependency is the switch and the property alone does nothing, so the application would go on
 * validating with the provider it has while appearing to have switched.
 *
 * @author Max Arulananthan
 * @since 2.3
 */
@AutoConfiguration
@ConditionalOnProperty(name = BeanValidationConfig.ENABLED_PROPERTY, havingValue = "true")
@ConditionalOnMissingClass(BeanValidationConfig.BEAN_VALIDATOR)
public class BeanValidationDependencyCheck {

    private static final Logger LOGGER = LoggerFactory.getLogger(BeanValidationDependencyCheck.class);

    /**
     * What is logged, once, at startup.
     */
    public static final String MESSAGE = BeanValidationConfig.ENABLED_PROPERTY + "=true has no effect: "
            + "org.rulii:rulii-bean-validation is not on the classpath. Add the dependency to validate with rulii.";

    public BeanValidationDependencyCheck() {
        super();
        LOGGER.warn(MESSAGE);
    }
}
