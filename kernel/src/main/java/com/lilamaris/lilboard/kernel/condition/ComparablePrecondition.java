package com.lilamaris.lilboard.kernel.condition;

import com.lilamaris.lilboard.kernel.exception.IllegalBoundsException;
import com.lilamaris.lilboard.kernel.exception.IllegalComparisonConditionException;
import java.util.Objects;

/** 자연 순서에 따른 범위 검사의 공통 구현. 양 끝 경계를 포함한다. */
final class ComparablePrecondition {
    private ComparablePrecondition() {}

    static <T extends Comparable<? super T>> T requireAtLeast(T value, T minimum) {
        Objects.requireNonNull(value, "value must not be null.");
        Objects.requireNonNull(minimum, "minimum must not be null.");
        if (value.compareTo(minimum) < 0) {
            throw new IllegalComparisonConditionException(
                    value, ComparisonCondition.AT_LEAST, minimum);
        }
        return value;
    }

    static <T extends Comparable<? super T>> T requireAtMost(T value, T maximum) {
        Objects.requireNonNull(value, "value must not be null.");
        Objects.requireNonNull(maximum, "maximum must not be null.");
        if (value.compareTo(maximum) > 0) {
            throw new IllegalComparisonConditionException(
                    value, ComparisonCondition.AT_MOST, maximum);
        }
        return value;
    }

    static <T extends Comparable<? super T>> T requireBetween(T value, T minimum, T maximum) {
        Objects.requireNonNull(value, "value must not be null.");
        Objects.requireNonNull(minimum, "minimum must not be null.");
        Objects.requireNonNull(maximum, "maximum must not be null.");
        if (minimum.compareTo(maximum) > 0) throw new IllegalBoundsException(minimum, maximum);
        if (value.compareTo(minimum) < 0 || value.compareTo(maximum) > 0) {
            throw new IllegalComparisonConditionException(
                    value, ComparisonCondition.BETWEEN, minimum, maximum);
        }
        return value;
    }
}
