package com.sqljudge.judge.compare;

@FunctionalInterface
public interface TestcaseExecutor {

    ExecutionOutcome execute(JudgeTestcase testcase, boolean revealToUser);
}
