package com.sqljudge.problem.dto.response;

import com.sqljudge.problem.entity.Dialect;
import com.sqljudge.problem.entity.Difficulty;
import com.sqljudge.problem.entity.StatementType;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ProblemDetailResponse(
        UUID id,
        int number,
        String slug,
        String title,
        Difficulty difficulty,
        List<TagResponse> tags,
        String statementMarkdown,
        List<Dialect> dialects,
        Map<Dialect, String> engineVersions,
        String starterCode,
        List<SchemaTableResponse> schema,
        List<ResultSetResponse> examples,
        boolean requiresOrder,
        List<StatementType> allowedStatements,
        int timeLimitMs,
        int hiddenTestcaseCount,
        double acceptanceRate,
        long solvedCount) {

    public record SchemaTableResponse(String name, List<SchemaColumnResponse> columns) {}

    public record SchemaColumnResponse(
            String name,
            String type,
            boolean isPrimaryKey,
            boolean isNullable,
            ColumnReferenceResponse references) {}

    public record ColumnReferenceResponse(String table, String column) {}

    public record ResultSetResponse(
            List<String> columns, List<List<String>> rows, boolean isTruncated) {}
}
