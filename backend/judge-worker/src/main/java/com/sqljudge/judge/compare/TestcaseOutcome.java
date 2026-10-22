package com.sqljudge.judge.compare;

public record TestcaseOutcome(int number, Verdict verdict, long runtimeMs, int rowCount) {}
