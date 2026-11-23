package com.sqljudge.problem.dto.response;

import com.sqljudge.problem.entity.Difficulty;
import com.sqljudge.problem.entity.ProblemStats;
import com.sqljudge.problem.entity.ProblemStatus;
import com.sqljudge.problem.entity.ProblemVisibility;
import com.sqljudge.problem.repository.AdminProblemRow;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminProblemSummaryResponse(
        UUID id,
        Integer number,
        String slug,
        String title,
        ProblemStatus status,
        ProblemVisibility visibility,
        Difficulty difficulty,
        UUID authorId,
        String authorUsername,
        Instant updatedAt,
        List<TagResponse> tags,
        long submissionCount,
        long acceptedCount,
        double acceptanceRate) {

    public static AdminProblemSummaryResponse from(
            AdminProblemRow row, List<TagResponse> tags, String authorUsername) {
        long submissions = row.submissionCount() == null ? 0 : row.submissionCount();
        long accepted = row.acceptedCount() == null ? 0 : row.acceptedCount();
        return new AdminProblemSummaryResponse(
                row.id(),
                row.number(),
                row.slug(),
                row.title(),
                row.status(),
                row.visibility(),
                row.difficulty(),
                row.authorId(),
                authorUsername,
                row.updatedAt(),
                tags,
                submissions,
                accepted,
                ProblemStats.acceptanceRate(submissions, accepted));
    }
}
