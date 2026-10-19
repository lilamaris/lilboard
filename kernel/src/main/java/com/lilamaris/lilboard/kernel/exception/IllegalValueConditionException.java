package com.lilamaris.lilboard.kernel.exception;

import com.lilamaris.lilboard.kernel.condition.Condition;
import java.util.List;

/** 값 자신의 상태로만 판단되는 조건 위반. 기준값이 없다. */
public final class IllegalValueConditionException extends IllegalConditionException {

    public IllegalValueConditionException(Object subject, Condition condition) {
        super(subject, condition, message(condition, List.of()));
    }
}
