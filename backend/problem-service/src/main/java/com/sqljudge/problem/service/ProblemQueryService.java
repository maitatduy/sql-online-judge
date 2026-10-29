package com.sqljudge.problem.service;

import com.sqljudge.common.security.SecurityUtils;
import com.sqljudge.common.web.PageResponse;
import com.sqljudge.problem.dto.response.ProblemProgress;
import com.sqljudge.problem.dto.response.ProblemSummaryResponse;
import com.sqljudge.problem.dto.response.TagResponse;
import com.sqljudge.problem.entity.ProblemProgressStatus;
import com.sqljudge.problem.entity.ProblemStats;
import com.sqljudge.problem.entity.ProblemStatus;
import com.sqljudge.problem.entity.ProblemVisibility;
import com.sqljudge.problem.repository.ProblemRepository;
import com.sqljudge.problem.repository.ProblemSummaryRow;
import com.sqljudge.problem.repository.ProblemTagRow;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProblemQueryService {

    private static final Pattern PROBLEM_NUMBER = Pattern.compile("\\d{1,9}");
    private static final Pattern LIKE_SPECIAL_CHARACTERS = Pattern.compile("([!%_])");

    private final ProblemRepository problemRepository;

    @Transactional(readOnly = true)
    public PageResponse<ProblemSummaryResponse> listPublished(
            ProblemSearch search, int page, int size) {
        UUID userId = SecurityUtils.currentUserId().orElse(null);
        String keyword = search.keyword() == null ? "" : search.keyword().trim();
        ProblemProgress progress = userId == null ? null : search.progress();
        Page<ProblemSummaryRow> rows =
                problemRepository.findListed(
                        ProblemStatus.PUBLISHED,
                        ProblemVisibility.PUBLIC,
                        userId,
                        search.difficulty(),
                        search.tag(),
                        keyword.isEmpty() ? null : titlePattern(keyword),
                        PROBLEM_NUMBER.matcher(keyword).matches() ? Integer.valueOf(keyword) : null,
                        progress == ProblemProgress.NOT_STARTED,
                        progressStatusOf(progress),
                        PageRequest.of(page - 1, size));
        Map<UUID, List<TagResponse>> tags = tagsOf(rows.getContent());
        return PageResponse.from(rows, row -> toSummary(row, tags, userId != null));
    }

    private Map<UUID, List<TagResponse>> tagsOf(List<ProblemSummaryRow> rows) {
        if (rows.isEmpty()) {
            return Map.of();
        }
        return problemRepository
                .findTagsOf(rows.stream().map(ProblemSummaryRow::id).toList())
                .stream()
                .collect(
                        Collectors.groupingBy(
                                ProblemTagRow::problemId,
                                Collectors.mapping(
                                        tag -> new TagResponse(tag.slug(), tag.name()),
                                        Collectors.toList())));
    }

    private static ProblemSummaryResponse toSummary(
            ProblemSummaryRow row, Map<UUID, List<TagResponse>> tags, boolean isSignedIn) {
        return new ProblemSummaryResponse(
                row.id(),
                row.number(),
                row.slug(),
                row.title(),
                row.difficulty(),
                tags.getOrDefault(row.id(), List.of()),
                ProblemStats.acceptanceRate(
                        zeroIfNull(row.submissionCount()), zeroIfNull(row.acceptedCount())),
                zeroIfNull(row.solvedUserCount()),
                isSignedIn ? progressOf(row.progress()) : null);
    }

    private static ProblemProgressStatus progressStatusOf(ProblemProgress progress) {
        if (progress == null || progress == ProblemProgress.NOT_STARTED) {
            return null;
        }
        return ProblemProgressStatus.valueOf(progress.name());
    }

    private static ProblemProgress progressOf(ProblemProgressStatus status) {
        return status == null
                ? ProblemProgress.NOT_STARTED
                : ProblemProgress.valueOf(status.name());
    }

    private static long zeroIfNull(Long value) {
        return value == null ? 0 : value;
    }

    private static String titlePattern(String keyword) {
        String lowered = keyword.toLowerCase(Locale.ROOT);
        return "%" + LIKE_SPECIAL_CHARACTERS.matcher(lowered).replaceAll("!$1") + "%";
    }
}
