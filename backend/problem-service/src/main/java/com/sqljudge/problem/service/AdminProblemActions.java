package com.sqljudge.problem.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.BusinessRuleException;
import com.sqljudge.common.exception.ConflictException;
import com.sqljudge.problem.client.AuthServiceClient;
import com.sqljudge.problem.client.ContestServiceClient;
import com.sqljudge.problem.client.UserAccount;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminProblemActions {

    private final AdminProblemCommandService commandService;
    private final StudioProblemEditService studioProblemEditService;
    private final ContestServiceClient contestServiceClient;
    private final AuthServiceClient authServiceClient;

    public void hide(UUID problemId) {
        commandService.requirePublished(problemId);
        requireNotInUnfinishedContest(problemId);
        commandService.hide(problemId);
    }

    public void delete(UUID problemId) {
        commandService.requirePublished(problemId);
        requireNotInUnfinishedContest(problemId);
        studioProblemEditService.delete(problemId);
    }

    public void transferOwnership(UUID problemId, String username) {
        commandService.requirePublished(problemId);
        UserAccount account = authServiceClient.account(username.trim());
        if (!account.isActiveSetter()) {
            throw new BusinessRuleException(
                    ErrorCode.NEW_OWNER_NOT_SETTER,
                    "Người nhận phải là người ra đề đang hoạt động");
        }
        commandService.transferOwnership(problemId, account.userId());
    }

    private void requireNotInUnfinishedContest(UUID problemId) {
        if (contestServiceClient.problemUsage(problemId).isInUnfinishedContest()) {
            throw new ConflictException(
                    ErrorCode.PROBLEM_IN_ACTIVE_CONTEST,
                    "Bài đang thuộc một contest chưa kết thúc");
        }
    }
}
