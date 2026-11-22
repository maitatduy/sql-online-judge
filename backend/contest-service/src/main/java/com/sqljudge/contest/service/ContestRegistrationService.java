package com.sqljudge.contest.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.BusinessRuleException;
import com.sqljudge.common.exception.ConflictException;
import com.sqljudge.common.exception.ForbiddenException;
import com.sqljudge.common.security.SecurityUtils;
import com.sqljudge.contest.client.AuthServiceClient;
import com.sqljudge.contest.client.ContestantInfo;
import com.sqljudge.contest.dto.response.RegistrationResponse;
import com.sqljudge.contest.entity.Contest;
import com.sqljudge.contest.entity.ContestPhase;
import com.sqljudge.contest.entity.ContestRegistration;
import com.sqljudge.contest.repository.ContestRegistrationRepository;
import com.sqljudge.contest.repository.ContestRepository;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContestRegistrationService {

    private final ContestRepository contestRepository;
    private final ContestRegistrationRepository registrationRepository;
    private final AuthServiceClient authServiceClient;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public RegistrationOutcome register(String slug) {
        UUID userId = SecurityUtils.requireCurrentUser().id();
        Contest contest = transactionTemplate.execute(status -> openContest(slug));
        RegistrationResponse existing = status(contest.getId(), userId);
        if (existing.isRegistered()) {
            return new RegistrationOutcome(existing, false);
        }
        ContestantInfo contestant = authServiceClient.user(userId);
        if (contest.isRated() && contestant.isBannedFromRatedContests()) {
            throw new ForbiddenException(
                    ErrorCode.RATED_CONTEST_BANNED,
                    "Tài khoản đang bị cấm tham gia contest tính rating");
        }
        try {
            return new RegistrationOutcome(save(slug, contestant), true);
        } catch (DataIntegrityViolationException duplicate) {
            return new RegistrationOutcome(status(contest.getId(), userId), false);
        }
    }

    public void unregister(String slug) {
        UUID userId = SecurityUtils.requireCurrentUser().id();
        transactionTemplate.executeWithoutResult(
                status -> {
                    Contest contest = findContest(slug);
                    if (contest.hasStartedAt(clock.instant())) {
                        throw new ConflictException(
                                ErrorCode.CONTEST_ALREADY_STARTED,
                                "Contest đã bắt đầu, không huỷ đăng ký được nữa");
                    }
                    registrationRepository
                            .findByContestIdAndUserId(contest.getId(), userId)
                            .ifPresent(registrationRepository::delete);
                });
    }

    public RegistrationResponse currentStatus(String slug) {
        UUID userId = SecurityUtils.requireCurrentUser().id();
        Contest contest = transactionTemplate.execute(status -> findContest(slug));
        return status(contest.getId(), userId);
    }

    private RegistrationResponse save(String slug, ContestantInfo contestant) {
        return transactionTemplate.execute(
                status -> {
                    Contest contest = openContest(slug);
                    ContestRegistration registration =
                            registrationRepository.saveAndFlush(
                                    ContestRegistration.of(
                                            contest,
                                            contestant.userId(),
                                            contestant.username(),
                                            contestant.displayName()));
                    log.info(
                            "contest_registered contestId={} userId={}",
                            contest.getId(),
                            contestant.userId());
                    return RegistrationResponse.of(registration);
                });
    }

    private RegistrationResponse status(UUID contestId, UUID userId) {
        return transactionTemplate.execute(
                status ->
                        registrationRepository
                                .findByContestIdAndUserId(contestId, userId)
                                .map(RegistrationResponse::of)
                                .orElseGet(RegistrationResponse::none));
    }

    private Contest openContest(String slug) {
        Contest contest = findContest(slug);
        if (contest.phaseAt(clock.instant()) == ContestPhase.FINISHED) {
            throw new BusinessRuleException(
                    ErrorCode.CONTEST_ALREADY_FINISHED, "Contest đã kết thúc, không đăng ký được");
        }
        return contest;
    }

    private Contest findContest(String slug) {
        return contestRepository.findBySlug(slug).orElseThrow(ContestQueryService::contestNotFound);
    }

    public record RegistrationOutcome(RegistrationResponse registration, boolean isCreated) {}
}
