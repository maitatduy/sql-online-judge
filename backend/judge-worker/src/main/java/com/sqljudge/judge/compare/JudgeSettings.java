package com.sqljudge.judge.compare;

public record JudgeSettings(CompareOptions compareOptions, boolean revealDiff) {

    public boolean mayReveal(JudgeTestcase testcase) {
        return revealDiff && !testcase.isHidden();
    }
}
