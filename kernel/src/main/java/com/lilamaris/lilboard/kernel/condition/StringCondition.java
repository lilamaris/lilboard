package com.lilamaris.lilboard.kernel.condition;

public enum StringCondition implements Condition {
    NOT_BLANK("not be blank");

    private final String phrase;

    StringCondition(String phrase) {
        this.phrase = phrase;
    }

    @Override
    public String phrase() {
        return phrase;
    }
}
