package com.sqljudge.problem.dto.response;

import com.sqljudge.problem.entity.Difficulty;
import com.sqljudge.problem.entity.Problem;
import com.sqljudge.problem.entity.ProblemRevision;
import com.sqljudge.problem.entity.ProblemStatus;
import com.sqljudge.problem.entity.ProblemVisibility;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record StudioProblemResponse(
        UUID id,
        Integer number,
        String slug,
        String title,
        ProblemStatus status,
        ProblemVisibility visibility,
        Difficulty difficulty,
        List<TagResponse> tags,
        String statementMarkdown,
        String editorialMarkdown,
        ProblemDatasetResponse dataset,
        ProblemReviewResponse latestReview,
        Instant updatedAt) {

    public StudioProblemResponse {
        tags = List.copyOf(tags);
    }

    public static StudioProblemResponse from(Problem problem) {
        return new StudioProblemResponse(
                problem.getId(),
                problem.getNumber(),
                problem.getSlug(),
                problem.getTitle(),
                problem.getStatus(),
                problem.getVisibility(),
                problem.getDifficulty(),
                problem.getTags().stream()
                        .map(TagResponse::from)
                        .sorted(Comparator.comparing(TagResponse::slug))
                        .toList(),
                problem.getStatementMarkdown(),
                problem.getEditorialMarkdown(),
                datasetOf(problem.getDraftRevision()),
                problem.latestReview().map(ProblemReviewResponse::from).orElse(null),
                problem.getUpdatedAt());
    }

    private static ProblemDatasetResponse datasetOf(ProblemRevision draft) {
        if (draft == null || draft.getDatasetVersion() == null) {
            return null;
        }
        return ProblemDatasetResponse.from(draft.getDatasetVersion().getDataset());
    }
}
