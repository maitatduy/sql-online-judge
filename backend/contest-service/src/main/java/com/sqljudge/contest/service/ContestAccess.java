package com.sqljudge.contest.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.ForbiddenException;
import com.sqljudge.common.security.CurrentUser;
import com.sqljudge.common.security.Role;
import com.sqljudge.contest.entity.Contest;
import com.sqljudge.contest.entity.ContestPhase;
import com.sqljudge.contest.repository.ContestRegistrationRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ContestAccess {

    private final ContestRegistrationRepository registrationRepository;

    void requireProblemAccess(Contest contest, CurrentUser user, Instant now) {
        ContestPhase phase = contest.phaseAt(now);
        if (phase == ContestPhase.UPCOMING) {
            throw new ForbiddenException(
                    ErrorCode.CONTEST_NOT_STARTED, "Contest chưa bắt đầu, chưa xem được đề");
        }
        if (phase == ContestPhase.FINISHED || user.hasRole(Role.ADMIN)) {
            return;
        }
        if (!registrationRepository.existsByContestIdAndUserId(contest.getId(), user.id())) {
            throw new ForbiddenException(
                    ErrorCode.CONTEST_NOT_REGISTERED, "Bạn cần đăng ký contest để xem đề");
        }
    }
}
