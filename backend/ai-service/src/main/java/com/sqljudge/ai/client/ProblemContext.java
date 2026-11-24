package com.sqljudge.ai.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;

public record ProblemContext(
        UUID problemId,
        String slug,
        String title,
        String statementMarkdown,
        List<String> dialects,
        List<SchemaTable> schema,
        @JsonProperty("isSolved") boolean isSolved) {

    public record SchemaTable(String name, List<SchemaColumn> columns) {}

    public record SchemaColumn(String name, String type) {}
}
