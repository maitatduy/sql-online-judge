package com.sqljudge.problem.repository;

import com.sqljudge.problem.entity.Difficulty;
import com.sqljudge.problem.entity.ProblemStatus;
import com.sqljudge.problem.entity.ProblemVisibility;
import java.time.Instant;
import java.util.UUID;

public record AdminProblemRow(
        UUID id,
        Integer number,
        String slug,
        String title,
        ProblemStatus status,
        ProblemVisibility visibility,
        Difficulty difficulty,
        UUID authorId,
        Instant updatedAt,
        Long submissionCount,
        Long acceptedCount) {}
