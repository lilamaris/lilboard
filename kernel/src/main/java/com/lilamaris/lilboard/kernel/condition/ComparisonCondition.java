package com.lilamaris.lilboard.kernel.condition;

public enum ComparisonCondition implements Condition {
    BEFORE("be before"),
    BEFORE_OR_EQUAL("be before or equal to"),
    AFTER("be after"),
    AFTER_OR_EQUAL("be after or equal to"),
    AT_LEAST("be at least"),
    AT_MOST("be at most"),
    BETWEEN("be between"),
    NOT_CONTAIN("not contain");

    private final String phrase;

    ComparisonCondition(String phrase) {
        this.phrase = phrase;
    }

    @Override
    public String phrase() {
        return phrase;
    }
}
