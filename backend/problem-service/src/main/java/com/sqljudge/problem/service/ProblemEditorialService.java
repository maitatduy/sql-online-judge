package com.sqljudge.problem.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.common.security.CurrentUser;
import com.sqljudge.common.security.Role;
import com.sqljudge.common.security.SecurityUtils;
import com.sqljudge.problem.dto.response.ProblemEditorialResponse;
import com.sqljudge.problem.entity.EditorialStatus;
import com.sqljudge.problem.entity.Problem;
import com.sqljudge.problem.entity.ProblemProgressStatus;
import com.sqljudge.problem.entity.ProblemStatus;
import com.sqljudge.problem.entity.ProblemVisibility;
import com.sqljudge.problem.repository.ProblemRepository;
import com.sqljudge.problem.repository.UserProblemProgressRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProblemEditorialService {

    private final ProblemRepository problemRepository;
    private final UserProblemProgressRepository progressRepository;

    @Transactional(readOnly = true)
    public ProblemEditorialResponse editorial(String slug) {
        Problem problem =
                problemRepository
                        .findBySlugAndStatusAndVisibilityAndDeletedAtIsNull(
                                slug, ProblemStatus.PUBLISHED, ProblemVisibility.PUBLIC)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                ErrorCode.PROBLEM_NOT_FOUND,
                                                "Không tìm thấy bài tập"));
        String editorial = problem.getEditorialMarkdown();
        if (editorial == null || editorial.isBlank()) {
            return ProblemEditorialResponse.withheld(EditorialStatus.NONE);
        }
        Optional<CurrentUser> viewer = SecurityUtils.currentUser();
        if (viewer.isPresent() && canRead(problem, viewer.get())) {
            return ProblemEditorialResponse.available(editorial);
        }
        return ProblemEditorialResponse.withheld(EditorialStatus.LOCKED);
    }

    private boolean canRead(Problem problem, CurrentUser viewer) {
        if (problem.getAuthorId().equals(viewer.id())
                || viewer.hasRole(Role.MODERATOR)
                || viewer.hasRole(Role.ADMIN)) {
            return true;
        }
        return progressRepository
                .findByUserIdAndProblemId(viewer.id(), problem.getId())
                .filter(progress -> progress.getStatus() == ProblemProgressStatus.SOLVED)
                .isPresent();
    }
}
