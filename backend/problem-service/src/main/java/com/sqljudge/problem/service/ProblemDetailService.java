package com.sqljudge.problem.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.problem.config.CacheConfig;
import com.sqljudge.problem.dto.response.ProblemDetailResponse;
import com.sqljudge.problem.dto.response.TagResponse;
import com.sqljudge.problem.entity.Dialect;
import com.sqljudge.problem.entity.Problem;
import com.sqljudge.problem.entity.ProblemRevision;
import com.sqljudge.problem.entity.ProblemStats;
import com.sqljudge.problem.entity.ProblemStatus;
import com.sqljudge.problem.entity.ProblemVisibility;
import com.sqljudge.problem.repository.ProblemRepository;
import com.sqljudge.problem.repository.ProblemStatsRepository;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProblemDetailService {

    private final ProblemRepository problemRepository;
    private final ProblemStatsRepository problemStatsRepository;
    private final ProblemStatementParts statementParts;

    @Cacheable(cacheNames = CacheConfig.PROBLEM_DETAIL, key = "#slug")
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
        List<Dialect> dialects = statementParts.dialectsOf(revision);
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
                statementParts.engineVersionsOf(dialects),
                problem.getStarterCode(),
                statementParts.schemaOf(revision),
                statementParts.examplesOf(revision),
                problem.isRequiresOrder(),
                problem.getAllowedStatements().stream().sorted().toList(),
                problem.getTimeLimitMs(),
                statementParts.hiddenTestcaseCountOf(revision),
                stats.acceptanceRate(),
                stats.getSolvedUserCount());
    }
}
