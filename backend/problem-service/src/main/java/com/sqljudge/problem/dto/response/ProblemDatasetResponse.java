package com.sqljudge.problem.dto.response;

import com.sqljudge.problem.entity.Dataset;
import java.util.UUID;

public record ProblemDatasetResponse(UUID id, String name) {

    public static ProblemDatasetResponse from(Dataset dataset) {
        return new ProblemDatasetResponse(dataset.getId(), dataset.getName());
    }
}
