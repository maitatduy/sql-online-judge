package com.sqljudge.ai.service;

import com.sqljudge.ai.client.ContestServiceClient;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.ForbiddenException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContestLock {

    private final ContestServiceClient contestServiceClient;

    public void ensureNotInRatedContest(UUID userId) {
        if (contestServiceClient.ratedParticipation(userId).isInRatedContest()) {
            throw new ForbiddenException(
                    ErrorCode.AI_DISABLED_DURING_CONTEST,
                    "Trợ lý AI tạm khoá khi bạn đang thi contest tính rating");
        }
    }
}
