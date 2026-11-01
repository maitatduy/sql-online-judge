package com.sqljudge.problem.dto.response;

import com.sqljudge.problem.entity.Difficulty;
import com.sqljudge.problem.entity.Problem;
import com.sqljudge.problem.entity.ProblemRevision;
import com.sqljudge.problem.entity.ProblemStatus;
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
        Difficulty difficulty,
        List<TagResponse> tags,
        String statementMarkdown,
        ProblemDatasetResponse dataset,
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
                problem.getDifficulty(),
                problem.getTags().stream()
                        .map(TagResponse::from)
                        .sorted(Comparator.comparing(TagResponse::slug))
                        .toList(),
                problem.getStatementMarkdown(),
                datasetOf(problem.getDraftRevision()),
                problem.getUpdatedAt());
    }

    private static ProblemDatasetResponse datasetOf(ProblemRevision draft) {
        if (draft == null || draft.getDatasetVersion() == null) {
            return null;
        }
        return ProblemDatasetResponse.from(draft.getDatasetVersion().getDataset());
    }
}
