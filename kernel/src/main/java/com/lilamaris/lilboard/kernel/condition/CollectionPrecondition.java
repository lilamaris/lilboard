package com.lilamaris.lilboard.kernel.condition;

import com.lilamaris.lilboard.kernel.exception.IllegalBoundsException;
import com.lilamaris.lilboard.kernel.exception.IllegalComparisonConditionException;
import com.lilamaris.lilboard.kernel.exception.IllegalValueConditionException;
import java.util.Collection;
import java.util.Objects;

/** 컬렉션 조건 검사. 크기 기준값은 0 이상이며 범위의 양 끝 경계를 포함한다. */
public final class CollectionPrecondition {

    private CollectionPrecondition() {}

    public static <E, C extends Collection<E>> C requireNonNullElements(C value) {
        Objects.requireNonNull(value, "value must not be null.");
        if (value.stream().anyMatch(Objects::isNull)) {
            throw new IllegalValueConditionException(value, CollectionCondition.NULL_ELEMENT);
        }
        return value;
    }

    public static <E, C extends Collection<E>> C requireSizeAtLeast(C value, int minimum) {
        Objects.requireNonNull(value, "value must not be null.");
        requireNonNegativeSize(minimum, "minimum");
        if (value.size() < minimum) {
            throw new IllegalComparisonConditionException(
                    value, CollectionCondition.SIZE_AT_LEAST, minimum);
        }
        return value;
    }

    public static <E, C extends Collection<E>> C requireSizeAtMost(C value, int maximum) {
        Objects.requireNonNull(value, "value must not be null.");
        requireNonNegativeSize(maximum, "maximum");
        if (value.size() > maximum) {
            throw new IllegalComparisonConditionException(
                    value, CollectionCondition.SIZE_AT_MOST, maximum);
        }
        return value;
    }

    public static <E, C extends Collection<E>> C requireSizeBetween(
            C value, int minimum, int maximum) {
        Objects.requireNonNull(value, "value must not be null.");
        requireNonNegativeSize(minimum, "minimum");
        requireNonNegativeSize(maximum, "maximum");
        if (minimum > maximum) throw new IllegalBoundsException(minimum, maximum);
        if (value.size() < minimum || value.size() > maximum) {
            throw new IllegalComparisonConditionException(
                    value, CollectionCondition.SIZE_BETWEEN, minimum, maximum);
        }
        return value;
    }

    private static void requireNonNegativeSize(int size, String name) {
        if (size < 0) throw new IllegalArgumentException(name + " must be non-negative.");
    }
}
