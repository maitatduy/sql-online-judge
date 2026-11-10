package com.sqljudge.problem.service;

import com.sqljudge.common.web.PageResponse;
import com.sqljudge.problem.dto.response.AdminProblemSummaryResponse;
import com.sqljudge.problem.entity.ProblemStatus;
import com.sqljudge.problem.repository.ProblemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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
            ProblemStatus status, int page, int size) {
        return PageResponse.from(
                problemRepository.findByStatusAndDeletedAtIsNull(
                        status, PageRequest.of(page - 1, size, OLDEST_UPDATE_FIRST)),
                AdminProblemSummaryResponse::from);
    }
}
