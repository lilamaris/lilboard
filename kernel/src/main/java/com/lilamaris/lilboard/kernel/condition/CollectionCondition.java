package com.lilamaris.lilboard.kernel.condition;

public enum CollectionCondition implements Condition {
    NULL_ELEMENT("not contain null elements"),
    SIZE_AT_LEAST("have at least"),
    SIZE_AT_MOST("have at most"),
    SIZE_BETWEEN("have between");

    private final String phrase;

    CollectionCondition(String phrase) {
        this.phrase = phrase;
    }

    @Override
    public String phrase() {
        return phrase;
    }
}
