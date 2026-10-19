package com.lilamaris.lilboard.kernel.condition;

public enum TimeCondition implements Condition {
    NEGATIVE("be negative"),
    NON_NEGATIVE("be non-negative"),
    POSITIVE("be positive"),
    NON_POSITIVE("be non-positive");

    private final String phrase;

    TimeCondition(String phrase) {
        this.phrase = phrase;
    }

    @Override
    public String phrase() {
        return phrase;
    }
}
