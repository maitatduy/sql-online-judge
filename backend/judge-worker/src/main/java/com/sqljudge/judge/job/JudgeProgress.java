package com.sqljudge.judge.job;

import java.util.UUID;

public record JudgeProgress(
        UUID submissionId, int attempt, int judgedTestcases, int totalTestcases) {}
