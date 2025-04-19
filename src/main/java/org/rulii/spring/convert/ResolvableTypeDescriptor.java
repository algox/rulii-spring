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
package org.rulii.spring.convert;

import org.springframework.core.ResolvableType;
import org.springframework.core.convert.TypeDescriptor;

/**
 * Represents a descriptor for a resolvable type.
 * Extends TypeDescriptor class and provides a constructor that takes a ResolvableType as a parameter.
 */
class ResolvableTypeDescriptor extends TypeDescriptor {

    /**
     * Constructs a ResolvableTypeDescriptor object with the given ResolvableType.
     *
     * @param resolvableType the ResolvableType object to be used in the ResolvableTypeDescriptor
     */
    ResolvableTypeDescriptor(ResolvableType resolvableType) {
        super(resolvableType, null, null);
    }
}

