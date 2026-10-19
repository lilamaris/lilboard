package com.lilamaris.lilboard.kernel.condition;

import com.lilamaris.lilboard.kernel.exception.IllegalComparisonConditionException;
import com.lilamaris.lilboard.kernel.exception.IllegalValueConditionException;
import java.time.Duration;
import java.util.Objects;

/** 시간 값의 자연 순서와 Duration의 부호를 검사한다. */
public final class TimePrecondition {

    private TimePrecondition() {}

    public static <T extends Comparable<? super T>> T requireBefore(T subject, T upperBound) {
        Objects.requireNonNull(subject, "subject must not be null.");
        Objects.requireNonNull(upperBound, "upperBound must not be null.");
        if (subject.compareTo(upperBound) >= 0) {
            throw new IllegalComparisonConditionException(
                    subject, ComparisonCondition.BEFORE, upperBound);
        }
        return subject;
    }

    public static <T extends Comparable<? super T>> T requireBeforeOrEqual(
            T subject, T upperBound) {
        Objects.requireNonNull(subject, "subject must not be null.");
        Objects.requireNonNull(upperBound, "upperBound must not be null.");
        if (subject.compareTo(upperBound) > 0) {
            throw new IllegalComparisonConditionException(
                    subject, ComparisonCondition.BEFORE_OR_EQUAL, upperBound);
        }
        return subject;
    }

    public static <T extends Comparable<? super T>> T requireAfter(T subject, T lowerBound) {
        Objects.requireNonNull(subject, "subject must not be null.");
        Objects.requireNonNull(lowerBound, "lowerBound must not be null.");
        if (subject.compareTo(lowerBound) <= 0) {
            throw new IllegalComparisonConditionException(
                    subject, ComparisonCondition.AFTER, lowerBound);
        }
        return subject;
    }

    public static <T extends Comparable<? super T>> T requireAfterOrEqual(T subject, T lowerBound) {
        Objects.requireNonNull(subject, "subject must not be null.");
        Objects.requireNonNull(lowerBound, "lowerBound must not be null.");
        if (subject.compareTo(lowerBound) < 0) {
            throw new IllegalComparisonConditionException(
                    subject, ComparisonCondition.AFTER_OR_EQUAL, lowerBound);
        }
        return subject;
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

    public static Duration requireNegative(Duration value) {
        Objects.requireNonNull(value, "value must not be null.");
        if (!value.isNegative())
            throw new IllegalValueConditionException(value, TimeCondition.NEGATIVE);
        return value;
    }

    public static Duration requireNonNegative(Duration value) {
        Objects.requireNonNull(value, "value must not be null.");
        if (value.isNegative())
            throw new IllegalValueConditionException(value, TimeCondition.NON_NEGATIVE);
        return value;
    }

    public static Duration requirePositive(Duration value) {
        Objects.requireNonNull(value, "value must not be null.");
        if (!value.isPositive())
            throw new IllegalValueConditionException(value, TimeCondition.POSITIVE);
        return value;
    }

    public static Duration requireNonPositive(Duration value) {
        Objects.requireNonNull(value, "value must not be null.");
        if (value.isPositive())
            throw new IllegalValueConditionException(value, TimeCondition.NON_POSITIVE);
        return value;
    }
}
