package com.sqljudge.problem.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sqljudge.problem.dto.response.ProblemDetailResponse.SchemaTableResponse;
import com.sqljudge.problem.entity.Dialect;
import java.util.List;
import java.util.UUID;

public record InternalAssistantContextResponse(
        UUID problemId,
        String slug,
        String title,
        String statementMarkdown,
        List<Dialect> dialects,
        List<SchemaTableResponse> schema,
        @JsonProperty("isSolved") boolean isSolved) {

    public InternalAssistantContextResponse {
        dialects = List.copyOf(dialects);
        schema = List.copyOf(schema);
    }
}
