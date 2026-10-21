package com.lilamaris.lilboard.kernel.exception;

/** 값 위반이 아니라 기준 자체의 정의 오류. 조건 위반 예외와는 의미가 다르므로 family와 분리한다. */
public final class IllegalBoundsException extends IllegalArgumentException {
    private final Object minimum;
    private final Object maximum;

    public IllegalBoundsException(Object minimum, Object maximum) {
        super("minimum must be less than or equal to maximum.");
        this.minimum = minimum;
        this.maximum = maximum;
    }

    public Object minimum() {
        return minimum;
    }

    public Object maximum() {
        return maximum;
    }
}
