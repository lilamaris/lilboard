package com.lilamaris.lilboard.kernel.exception;

import com.lilamaris.lilboard.kernel.condition.Condition;
import java.util.List;
import java.util.stream.Collectors;

/** 조건 검사 실패의 공통 조상. 검사 대상(subject)과 위반된 조건(condition)을 담는다. 메시지는 조건만 담고, 검사 대상의 구체적인 이름은 담지 않는다. */
public abstract sealed class IllegalConditionException extends IllegalArgumentException
        permits IllegalValueConditionException, IllegalComparisonConditionException {

    private final Object subject;
    private final Condition condition;

    protected IllegalConditionException(Object subject, Condition condition, String message) {
        super(message);
        this.subject = subject;
        this.condition = condition;
    }

    protected static String message(Condition condition, List<?> references) {
        String detail =
                references.isEmpty()
                        ? ""
                        : " "
                                + references.stream()
                                        .map(String::valueOf)
                                        .collect(
                                                Collectors.joining(
                                                        references.size() > 1 ? " and " : " "));
        return "must " + condition.phrase() + detail + ".";
    }

    /** 검사 대상. */
    public Object subject() {
        return subject;
    }

    public Condition condition() {
        return condition;
    }
}
