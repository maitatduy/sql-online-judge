package com.sqljudge.auth.service;

import com.sqljudge.auth.dto.response.AccountStatus;
import com.sqljudge.auth.dto.response.AdminUserSummaryResponse;
import com.sqljudge.auth.dto.response.UserCountsResponse;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.repository.UserCounts;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.common.web.PageResponse;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserQueryService {

    private static final Sort NEWEST_FIRST =
            Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
    private static final int STAT_NOT_TRACKED_YET = 0;
    private static final boolean PRESENCE_NOT_TRACKED_YET = false;
    private static final String AVATAR_NOT_STORED_YET = null;

    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<AdminUserSummaryResponse> search(
            AdminUserSearch search, int page, int size) {
        Instant now = clock.instant();
        return PageResponse.from(
                userRepository.findAll(
                        AdminUserSpecifications.matching(search, now),
                        PageRequest.of(page - 1, size, NEWEST_FIRST)),
                user -> toSummary(user, now));
    }

    @Transactional(readOnly = true)
    public UserCountsResponse countUsers() {
        UserCounts counts = userRepository.countLiveUsers(clock.instant());
        return new UserCountsResponse(
                counts.total(),
                counts.unverified(),
                counts.setters(),
                counts.moderators(),
                counts.banned());
    }

    private static AdminUserSummaryResponse toSummary(User user, Instant now) {
        return new AdminUserSummaryResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                STAT_NOT_TRACKED_YET,
                STAT_NOT_TRACKED_YET,
                user.getCreatedAt(),
                user.getLastActiveAt(),
                PRESENCE_NOT_TRACKED_YET,
                user.isBannedAt(now) ? AccountStatus.BANNED : AccountStatus.ACTIVE,
                user.getBannedUntil(),
                user.isEmailVerified(),
                AVATAR_NOT_STORED_YET,
                user.getDeletedAt());
    }
}
