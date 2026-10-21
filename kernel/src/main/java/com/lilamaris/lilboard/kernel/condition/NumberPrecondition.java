package com.lilamaris.lilboard.kernel.condition;

import com.lilamaris.lilboard.kernel.exception.IllegalValueConditionException;
import java.util.Objects;

public final class NumberPrecondition {
    private NumberPrecondition() {}

    public static Integer requireNegative(Integer value) {
        Objects.requireNonNull(value, "value must not be null.");
        if (value >= 0) throw new IllegalValueConditionException(value, NumberCondition.NEGATIVE);
        return value;
    }

    public static Integer requireNonNegative(Integer value) {
        Objects.requireNonNull(value, "value must not be null.");
        if (value < 0)
            throw new IllegalValueConditionException(value, NumberCondition.NON_NEGATIVE);
        return value;
    }

    public static Integer requirePositive(Integer value) {
        Objects.requireNonNull(value, "value must not be null.");
        if (value <= 0) throw new IllegalValueConditionException(value, NumberCondition.POSITIVE);
        return value;
    }

    public static Integer requireNonPositive(Integer value) {
        Objects.requireNonNull(value, "value must not be null.");
        if (value > 0)
            throw new IllegalValueConditionException(value, NumberCondition.NON_POSITIVE);
        return value;
    }

    public static Long requireNegative(Long value) {
        Objects.requireNonNull(value, "value must not be null.");
        if (value >= 0) throw new IllegalValueConditionException(value, NumberCondition.NEGATIVE);
        return value;
    }

    public static Long requireNonNegative(Long value) {
        Objects.requireNonNull(value, "value must not be null.");
        if (value < 0)
            throw new IllegalValueConditionException(value, NumberCondition.NON_NEGATIVE);
        return value;
    }

    public static Long requirePositive(Long value) {
        Objects.requireNonNull(value, "value must not be null.");
        if (value <= 0) throw new IllegalValueConditionException(value, NumberCondition.POSITIVE);
        return value;
    }

    public static Long requireNonPositive(Long value) {
        Objects.requireNonNull(value, "value must not be null.");
        if (value > 0)
            throw new IllegalValueConditionException(value, NumberCondition.NON_POSITIVE);
        return value;
    }

    public static <T extends Comparable<? super T>> T requireAtLeast(T value, T minimum) {
        return ComparablePrecondition.requireAtLeast(value, minimum);
    }

    public static <T extends Comparable<? super T>> T requireAtMost(T value, T maximum) {
        return ComparablePrecondition.requireAtMost(value, maximum);
    }

    public static <T extends Comparable<? super T>> T requireBetween(
            T value, T minimum, T maximum) {
        return ComparablePrecondition.requireBetween(value, minimum, maximum);
    }
}
