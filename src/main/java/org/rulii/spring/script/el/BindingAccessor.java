package org.rulii.spring.script.el;

import org.rulii.bind.Binding;
import org.rulii.bind.Bindings;
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
 * <p>Property access is read-only; write operations are not supported.</p>
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
     * Returns {@code true} if the target {@link Bindings} contains a binding with the given name.
     *
     * @param context the evaluation context.
     * @param target  the root object; must be a {@link Bindings} instance.
     * @param name    the property (binding) name to look up.
     * @return {@code true} if the binding exists.
     */
    @Override
    public boolean canRead(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
        return target instanceof Bindings && ((Bindings) target).contains(name);
    }

    /**
     * Reads the value of the named binding from the target {@link Bindings}.
     *
     * @param context the evaluation context.
     * @param target  the root object; must be a {@link Bindings} instance.
     * @param name    the binding name to read.
     * @return a {@link TypedValue} wrapping the binding's current value,
     *         or {@link TypedValue#NULL} if the binding has no value.
     * @throws AccessException if the binding cannot be read.
     */
    @Override
    public TypedValue read(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
        if (!(target instanceof Bindings bindings)) return TypedValue.NULL;

        Binding<?> binding = bindings.getBinding(name);
        return binding == null ? TypedValue.NULL : new TypedValue(binding.getValue());
    }

    /**
     * Always returns {@code false} — bindings are read-only via property access.
     */
    @Override
    public boolean canWrite(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
        if (!(target instanceof Bindings bindings)) return false;

        Binding<?> binding = bindings.getBinding(name);
        return binding == null || binding.isEditable();
    }

    /**
     * Not supported; throws {@link AccessException} if called.
     */
    @Override
    public void write(EvaluationContext context, @Nullable Object target, String name, @Nullable Object newValue) throws AccessException {
        if (!(target instanceof Bindings bindings)) return;
        bindings.setValueOrBind(name, newValue);
    }
}
