package com.lilamaris.lilboard.kernel.condition;

/** 조건 어휘. 메시지는 "must " + phrase() + references 형태로 조립된다. */
public interface Condition {
    String phrase();
}
