package com.lilamaris.lilboard.kernel.condition;

import com.lilamaris.lilboard.kernel.exception.IllegalComparisonConditionException;
import com.lilamaris.lilboard.kernel.exception.IllegalValueConditionException;
import java.util.Objects;

public final class StringPrecondition {

    private StringPrecondition() {}

    public static String requireNonBlank(String value) {
        Objects.requireNonNull(value, "value must not be null.");
        if (value.isBlank())
            throw new IllegalValueConditionException(value, StringCondition.NOT_BLANK);
        return value;
    }

    public static String requireNotContain(String value, String text) {
        requireNonBlank(value);
        Objects.requireNonNull(text, "text must not be null.");
        if (value.contains(text)) {
            throw new IllegalComparisonConditionException(
                    value, ComparisonCondition.NOT_CONTAIN, text);
        }
        return value;
    }

    public static String requireMaxLen(String value, int maxLen) {
        requireNonBlank(value);
        NumberPrecondition.requireAtMost(value.length(), maxLen);
        return value;
    }
}
