package com.sqljudge.problem.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.problem.config.JudgeEngineProperties;
import com.sqljudge.problem.dto.response.ProblemDetailResponse.ResultSetResponse;
import com.sqljudge.problem.dto.response.ProblemDetailResponse.SchemaTableResponse;
import com.sqljudge.problem.entity.DatasetVersion;
import com.sqljudge.problem.entity.Dialect;
import com.sqljudge.problem.entity.ExpectedResult;
import com.sqljudge.problem.entity.ProblemRevision;
import com.sqljudge.problem.entity.ReferenceSolution;
import com.sqljudge.problem.entity.Testcase;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ProblemStatementParts {

    private static final TypeReference<List<String>> COLUMNS = new TypeReference<>() {};
    private static final TypeReference<List<List<JsonNode>>> ROWS = new TypeReference<>() {};

    private final JudgeEngineProperties judgeEngineProperties;
    private final ObjectMapper objectMapper;

    List<Dialect> dialectsOf(ProblemRevision revision) {
        return revision.getReferenceSolutions().stream()
                .map(ReferenceSolution::getDialect)
                .sorted()
                .toList();
    }

    Map<Dialect, String> engineVersionsOf(List<Dialect> dialects) {
        Map<Dialect, String> versions = new EnumMap<>(Dialect.class);
        dialects.forEach(
                dialect ->
                        versions.put(dialect, judgeEngineProperties.engineVersions().get(dialect)));
        return versions;
    }

    List<SchemaTableResponse> schemaOf(ProblemRevision revision) {
        DatasetVersion datasetVersion = revision.getDatasetVersion();
        return datasetVersion == null ? List.of() : DatasetSchemas.tablesOf(datasetVersion);
    }

    List<ResultSetResponse> examplesOf(ProblemRevision revision) {
        return revision.getTestcases().stream()
                .filter(Testcase::isPublic)
                .map(testcase -> toResultSet(testcase.getExpectedResult()))
                .toList();
    }

    int hiddenTestcaseCountOf(ProblemRevision revision) {
        return (int)
                revision.getTestcases().stream().filter(testcase -> !testcase.isPublic()).count();
    }

    private ResultSetResponse toResultSet(ExpectedResult result) {
        try {
            return new ResultSetResponse(
                    objectMapper.readValue(result.getColumnsJson(), COLUMNS),
                    objectMapper.readValue(result.getRowsJson(), ROWS).stream()
                            .map(row -> row.stream().map(ProblemStatementParts::displayOf).toList())
                            .toList(),
                    false);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored expected result is not valid JSON", exception);
        }
    }

    private static String displayOf(JsonNode cell) {
        JsonNode value = cell.isObject() ? cell.path("value") : cell;
        return value.isNull() || value.isMissingNode() ? null : value.asText();
    }
}
