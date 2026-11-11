package com.sqljudge.judge.job;

import com.sqljudge.judge.sandbox.Dialect;
import java.util.UUID;

public record SubmissionJudgeRequest(
        UUID submissionId, int attempt, Dialect dialect, String code, JudgePackage judgePackage) {}
