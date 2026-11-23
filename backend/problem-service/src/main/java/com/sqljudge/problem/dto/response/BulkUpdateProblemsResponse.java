package com.sqljudge.problem.dto.response;

import java.util.List;
import java.util.UUID;

public record BulkUpdateProblemsResponse(List<UUID> succeeded, List<Failure> failed) {

    public record Failure(UUID problemId, String code, String message) {}
}
