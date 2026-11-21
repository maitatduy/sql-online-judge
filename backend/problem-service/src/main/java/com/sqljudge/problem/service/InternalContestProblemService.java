package com.sqljudge.problem.service;

import com.sqljudge.problem.dto.response.InternalContestProblemResponse;
import com.sqljudge.problem.entity.ProblemStatus;
import com.sqljudge.problem.entity.ProblemVisibility;
import com.sqljudge.problem.repository.ProblemRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InternalContestProblemService {

    private final ProblemRepository problemRepository;

    @Transactional(readOnly = true)
    public List<InternalContestProblemResponse> contestReady(List<UUID> problemIds) {
        return problemRepository
                .findByIdInAndStatusAndVisibilityAndDeletedAtIsNull(
                        problemIds, ProblemStatus.PUBLISHED, ProblemVisibility.CONTEST_ONLY)
                .stream()
                .map(InternalContestProblemResponse::from)
                .toList();
    }
}
