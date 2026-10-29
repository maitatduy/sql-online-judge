package com.sqljudge.problem.repository;

import com.sqljudge.problem.entity.Difficulty;
import com.sqljudge.problem.entity.ProblemProgressStatus;
import java.util.UUID;

public record ProblemSummaryRow(
        UUID id,
        int number,
        String slug,
        String title,
        Difficulty difficulty,
        Long submissionCount,
        Long acceptedCount,
        Long solvedUserCount,
        ProblemProgressStatus progress) {}
