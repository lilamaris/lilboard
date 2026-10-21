package com.lilamaris.lilboard.kernel.exception;

import com.lilamaris.lilboard.kernel.condition.Condition;
import java.util.List;

/** 값과 기준값(reference)의 관계로 판단되는 조건 위반. 하나 이상의 null이 아닌 기준값을 순서대로 담는다. */
public final class IllegalComparisonConditionException extends IllegalConditionException {
    private final List<?> references;

    public IllegalComparisonConditionException(
            Object subject, Condition condition, Object... references) {
        this(subject, condition, requireReferences(references));
    }

    private IllegalComparisonConditionException(
            Object subject, Condition condition, List<?> references) {
        super(subject, condition, message(condition, references));
        this.references = references;
    }

    public List<?> references() {
        return references;
    }

    public Object reference() {
        return references.getFirst();
    }

    private static List<?> requireReferences(Object[] references) {
        if (references.length == 0) {
            throw new IllegalArgumentException("at least one reference is required.");
        }
        return List.of(references);
    }
}
