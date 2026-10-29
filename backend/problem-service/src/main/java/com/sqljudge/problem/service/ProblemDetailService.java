package com.sqljudge.problem.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.problem.config.JudgeEngineProperties;
import com.sqljudge.problem.dto.response.ProblemDetailResponse;
import com.sqljudge.problem.dto.response.ProblemDetailResponse.ColumnReferenceResponse;
import com.sqljudge.problem.dto.response.ProblemDetailResponse.ResultSetResponse;
import com.sqljudge.problem.dto.response.ProblemDetailResponse.SchemaColumnResponse;
import com.sqljudge.problem.dto.response.ProblemDetailResponse.SchemaTableResponse;
import com.sqljudge.problem.dto.response.TagResponse;
import com.sqljudge.problem.entity.Dialect;
import com.sqljudge.problem.entity.ExpectedResult;
import com.sqljudge.problem.entity.Problem;
import com.sqljudge.problem.entity.ProblemRevision;
import com.sqljudge.problem.entity.ProblemStats;
import com.sqljudge.problem.entity.ProblemStatus;
import com.sqljudge.problem.entity.ProblemVisibility;
import com.sqljudge.problem.entity.ReferenceSolution;
import com.sqljudge.problem.entity.RevisionColumn;
import com.sqljudge.problem.entity.RevisionTable;
import com.sqljudge.problem.entity.Testcase;
import com.sqljudge.problem.repository.ProblemRepository;
import com.sqljudge.problem.repository.ProblemStatsRepository;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProblemDetailService {

    private static final TypeReference<List<String>> COLUMNS = new TypeReference<>() {};
    private static final TypeReference<List<List<String>>> ROWS = new TypeReference<>() {};

    private final ProblemRepository problemRepository;
    private final ProblemStatsRepository problemStatsRepository;
    private final JudgeEngineProperties judgeEngineProperties;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public ProblemDetailResponse getPublished(String slug) {
        Problem problem =
                problemRepository
                        .findBySlugAndStatusAndVisibilityAndDeletedAtIsNull(
                                slug, ProblemStatus.PUBLISHED, ProblemVisibility.PUBLIC)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                ErrorCode.PROBLEM_NOT_FOUND,
                                                "Không tìm thấy bài tập"));
        ProblemRevision revision = problem.getPublishedRevision();
        List<Dialect> dialects =
                revision.getReferenceSolutions().stream()
                        .map(ReferenceSolution::getDialect)
                        .sorted()
                        .toList();
        ProblemStats stats =
                problemStatsRepository
                        .findByProblemId(problem.getId())
                        .orElseGet(() -> new ProblemStats(problem.getId(), 0, 0, 0));
        return new ProblemDetailResponse(
                problem.getId(),
                problem.getNumber(),
                problem.getSlug(),
                problem.getTitle(),
                problem.getDifficulty(),
                problem.getTags().stream()
                        .map(TagResponse::from)
                        .sorted(Comparator.comparing(TagResponse::name))
                        .toList(),
                problem.getStatementMarkdown(),
                dialects,
                engineVersionsOf(dialects),
                problem.getStarterCode(),
                revision.getTables().stream().map(ProblemDetailService::toSchemaTable).toList(),
                revision.getTestcases().stream()
                        .filter(Testcase::isPublic)
                        .map(testcase -> toResultSet(testcase.getExpectedResult()))
                        .toList(),
                problem.isRequiresOrder(),
                problem.getAllowedStatements().stream().sorted().toList(),
                problem.getTimeLimitMs(),
                (int)
                        revision.getTestcases().stream()
                                .filter(testcase -> !testcase.isPublic())
                                .count(),
                stats.acceptanceRate(),
                stats.getSolvedUserCount());
    }

    private Map<Dialect, String> engineVersionsOf(List<Dialect> dialects) {
        Map<Dialect, String> versions = new EnumMap<>(Dialect.class);
        dialects.forEach(
                dialect ->
                        versions.put(dialect, judgeEngineProperties.engineVersions().get(dialect)));
        return versions;
    }

    private ResultSetResponse toResultSet(ExpectedResult result) {
        try {
            return new ResultSetResponse(
                    objectMapper.readValue(result.getColumnsJson(), COLUMNS),
                    objectMapper.readValue(result.getRowsJson(), ROWS),
                    false);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored expected result is not valid JSON", exception);
        }
    }

    private static SchemaTableResponse toSchemaTable(RevisionTable table) {
        return new SchemaTableResponse(
                table.getName(),
                table.getColumns().stream().map(ProblemDetailService::toSchemaColumn).toList());
    }

    private static SchemaColumnResponse toSchemaColumn(RevisionColumn column) {
        ColumnReferenceResponse references =
                column.getReferencesTable() == null
                        ? null
                        : new ColumnReferenceResponse(
                                column.getReferencesTable(), column.getReferencesColumn());
        return new SchemaColumnResponse(
                column.getName(),
                column.getLogicalType().name(),
                column.isPrimaryKey(),
                column.isNullable(),
                references);
    }
}
