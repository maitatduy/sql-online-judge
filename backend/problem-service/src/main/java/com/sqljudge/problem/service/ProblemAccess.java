package com.sqljudge.problem.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.BusinessRuleException;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.common.security.CurrentUser;
import com.sqljudge.common.security.Role;
import com.sqljudge.common.security.SecurityUtils;
import com.sqljudge.problem.entity.Problem;
import com.sqljudge.problem.repository.ProblemRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProblemAccess {

    private final ProblemRepository problemRepository;

    public Problem accessible(UUID problemId) {
        CurrentUser user = SecurityUtils.requireCurrentUser();
        return problemRepository
                .findByIdAndDeletedAtIsNull(problemId)
                .filter(
                        problem ->
                                problem.getAuthorId().equals(user.id()) || user.hasRole(Role.ADMIN))
                .orElseThrow(
                        () ->
                                new NotFoundException(
                                        ErrorCode.PROBLEM_NOT_FOUND, "Không tìm thấy bài tập"));
    }

    public Problem editable(UUID problemId) {
        Problem problem = accessible(problemId);
        if (!problem.isEditableBySetter()) {
            throw new BusinessRuleException(
                    ErrorCode.PROBLEM_LOCKED, "Bài đang chờ duyệt hoặc đã xuất bản, chưa sửa được");
        }
        return problem;
    }
}
