package com.sqljudge.problem.service;

import com.sqljudge.common.web.PageResponse;
import com.sqljudge.problem.dto.response.AdminProblemSummaryResponse;
import com.sqljudge.problem.entity.Problem;
import com.sqljudge.problem.entity.ProblemStatus;
import com.sqljudge.problem.entity.ProblemVisibility;
import com.sqljudge.problem.repository.ProblemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminProblemQueryService {

    private static final Sort OLDEST_UPDATE_FIRST = Sort.by("updatedAt", "id");

    private final ProblemRepository problemRepository;

    @Transactional(readOnly = true)
    public PageResponse<AdminProblemSummaryResponse> list(
            ProblemStatus status, ProblemVisibility visibility, int page, int size) {
        return PageResponse.from(
                find(status, visibility, PageRequest.of(page - 1, size, OLDEST_UPDATE_FIRST)),
                AdminProblemSummaryResponse::from);
    }

    private Page<Problem> find(
            ProblemStatus status, ProblemVisibility visibility, Pageable pageable) {
        return visibility == null
                ? problemRepository.findByStatusAndDeletedAtIsNull(status, pageable)
                : problemRepository.findByStatusAndVisibilityAndDeletedAtIsNull(
                        status, visibility, pageable);
    }
}
