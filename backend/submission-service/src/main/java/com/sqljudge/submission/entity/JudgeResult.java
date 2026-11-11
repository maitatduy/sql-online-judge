package com.sqljudge.submission.entity;

public record JudgeResult(
        int attempt,
        Verdict verdict,
        long runtimeMs,
        Integer failedIndex,
        Boolean failedIsHidden,
        String message,
        int judgedTestcases) {}
