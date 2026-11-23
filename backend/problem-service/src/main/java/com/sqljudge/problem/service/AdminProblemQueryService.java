package com.sqljudge.problem.service;

import com.sqljudge.common.web.PageResponse;
import com.sqljudge.problem.dto.response.AdminProblemBankSummaryResponse;
import com.sqljudge.problem.dto.response.AdminProblemSummaryResponse;
import com.sqljudge.problem.dto.response.TagResponse;
import com.sqljudge.problem.entity.ProblemStatus;
import com.sqljudge.problem.entity.ProblemVisibility;
import com.sqljudge.problem.repository.AdminProblemRow;
import com.sqljudge.problem.repository.ProblemRepository;
import com.sqljudge.problem.repository.ProblemTagRow;
import com.sqljudge.problem.repository.VisibilityCountRow;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminProblemQueryService {

    public static final String SORT_BY_NUMBER = "number";
    private static final Sort OLDEST_UPDATE_FIRST = Sort.by("updatedAt", "id");
    private static final Sort BY_NUMBER = Sort.by("number", "id");
    private static final Pattern PROBLEM_NUMBER = Pattern.compile("\\d{1,9}");
    private static final long LOW_ACCEPTANCE_MIN_SUBMISSIONS = 50;
    private static final long LOW_ACCEPTANCE_MAX_PERCENT = 10;

    private final ProblemRepository problemRepository;

    @Transactional(readOnly = true)
    public PageResponse<AdminProblemSummaryResponse> list(
            AdminProblemSearch search, int page, int size) {
        String keyword = search.keyword() == null ? "" : search.keyword().trim();
        Sort sort = SORT_BY_NUMBER.equals(search.sort()) ? BY_NUMBER : OLDEST_UPDATE_FIRST;
        Page<AdminProblemRow> rows =
                problemRepository.findForAdmin(
                        search.status(),
                        search.visibility(),
                        search.difficulty(),
                        search.tag(),
                        LikePatterns.containingIgnoreCase(keyword),
                        PROBLEM_NUMBER.matcher(keyword).matches() ? Integer.valueOf(keyword) : null,
                        PageRequest.of(page - 1, size, sort));
        Map<UUID, List<TagResponse>> tags = tagsOf(rows.getContent());
        return PageResponse.from(
                rows,
                row ->
                        AdminProblemSummaryResponse.from(
                                row, tags.getOrDefault(row.id(), List.of())));
    }

    @Transactional(readOnly = true)
    public AdminProblemBankSummaryResponse bankSummary() {
        Map<ProblemVisibility, Long> counts =
                problemRepository.countByVisibility(ProblemStatus.PUBLISHED).stream()
                        .filter(row -> row.getVisibility() != null)
                        .collect(
                                Collectors.toMap(
                                        VisibilityCountRow::getVisibility,
                                        VisibilityCountRow::getTotal));
        return new AdminProblemBankSummaryResponse(
                counts.getOrDefault(ProblemVisibility.PUBLIC, 0L),
                counts.getOrDefault(ProblemVisibility.CONTEST_ONLY, 0L),
                counts.getOrDefault(ProblemVisibility.HIDDEN, 0L),
                problemRepository.countLowAcceptance(
                        ProblemStatus.PUBLISHED,
                        LOW_ACCEPTANCE_MIN_SUBMISSIONS,
                        LOW_ACCEPTANCE_MAX_PERCENT));
    }

    private Map<UUID, List<TagResponse>> tagsOf(List<AdminProblemRow> rows) {
        if (rows.isEmpty()) {
            return Map.of();
        }
        return problemRepository
                .findTagsOf(rows.stream().map(AdminProblemRow::id).toList())
                .stream()
                .collect(
                        Collectors.groupingBy(
                                ProblemTagRow::problemId,
                                Collectors.mapping(
                                        tag -> new TagResponse(tag.slug(), tag.name()),
                                        Collectors.toList())));
    }
}
