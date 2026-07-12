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
package org.rulii.spring.script.el;

import org.rulii.bind.Binding;
import org.rulii.bind.Bindings;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.lang.Nullable;

/**
 * A Spring SpEL {@link PropertyAccessor} that resolves property names against a {@link Bindings} target.
 *
 * <p>When registered and the root object of the evaluation context is a {@link Bindings} instance,
 * this accessor allows bare names (e.g. {@code age}) in SpEL expressions to be resolved directly
 * from the bindings — complementing the {@code #age} variable-style access provided by
 * {@link EvaluationContext#lookupVariable}.</p>
 *
 * <p>Property access is read-write: reads return the binding's current value together with its
 * declared type, and writes go through {@link Bindings#setValueOrBind} — an existing editable
 * binding is updated, a missing name is bound as a <em>new</em> binding, and non-editable or
 * immutable bindings reject the write. Note that assigning to a misspelled name therefore
 * silently creates a new binding rather than failing.</p>
 *
 * @author Max Arulananthan
 * @since 1.1
 * @see EvaluationContext
 */
public class BindingAccessor implements PropertyAccessor {

    public BindingAccessor() {
        super();
    }

    /**
     * Restricts this accessor to {@link Bindings} targets only.
     *
     * @return an array containing {@code Bindings.class}.
     */
    @Override
    public Class<?>[] getSpecificTargetClasses() {
        return new Class<?>[] { Bindings.class };
    }

    /**
     * Returns {@code true} for any name on a {@link Bindings} target: an absent binding
     * reads as {@code null} (see {@link #read}), so guard expressions like
     * {@code #ctx.name != null} evaluate consistently instead of erroring. Claiming every
     * name also keeps the reflective accessor from resolving the {@code Bindings} object's
     * own JavaBean properties as if they were bindings.
     *
     * @param context the evaluation context.
     * @param target  the root object; must be a {@link Bindings} instance.
     * @param name    the property (binding) name to look up.
     * @return {@code true} if the target is a {@link Bindings}.
     */
    @Override
    public boolean canRead(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
        return target instanceof Bindings;
    }

    /**
     * Reads the value of the named binding from the target {@link Bindings}.
     *
     * <p>An absent binding reads as {@link TypedValue#NULL} — the engine's null-for-missing
     * semantics that guard expressions rely on. A present binding's value is returned
     * together with its <em>declared</em> type (including generics), so SpEL type
     * conversion sees more than the runtime class.
     *
     * @param context the evaluation context.
     * @param target  the root object; must be a {@link Bindings} instance.
     * @param name    the binding name to read.
     * @return a {@link TypedValue} wrapping the binding's current value and declared type,
     *         or {@link TypedValue#NULL} when the binding is absent.
     * @throws AccessException if the target is not a {@link Bindings} instance.
     */
    @Override
    public TypedValue read(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
        if (!(target instanceof Bindings bindings)) {
            throw new AccessException("Cannot read [" + name + "]: target is not a Bindings instance.");
        }

        Binding<?> binding = bindings.getBinding(name);

        if (binding == null) return TypedValue.NULL;

        return new TypedValue(binding.getValue(),
                new TypeDescriptor(ResolvableType.forType(binding.getType()), null, null));
    }

    /**
     * Returns {@code true} when the named binding is editable, or does not exist yet
     * (in which case {@link #write} will bind it as a new binding).
     */
    @Override
    public boolean canWrite(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
        if (!(target instanceof Bindings bindings)) return false;

        Binding<?> binding = bindings.getBinding(name);
        return binding == null || binding.isEditable();
    }

    /**
     * Writes through to {@link Bindings#setValueOrBind}: updates an existing editable
     * binding, or binds a new one when the name is absent.
     *
     * @throws AccessException if the target is not a {@link Bindings}, or the underlying
     *                         binding rejects the write (non-editable/immutable).
     */
    @Override
    public void write(EvaluationContext context, @Nullable Object target, String name, @Nullable Object newValue) throws AccessException {
        if (!(target instanceof Bindings bindings)) {
            throw new AccessException("Cannot write [" + name + "]: target is not a Bindings instance.");
        }

        try {
            bindings.setValueOrBind(name, newValue);
        } catch (RuntimeException e) {
            throw new AccessException("Failed to write binding [" + name + "]: " + e.getMessage(), e);
        }
    }
}
