package com.sqljudge.auth.service;

import com.sqljudge.auth.dto.response.AdminUserDetailResponse;
import com.sqljudge.auth.dto.response.AdminUserSummaryResponse;
import com.sqljudge.auth.dto.response.UserCountsResponse;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.repository.UserCounts;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.NotFoundException;
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
                user -> AdminUserViews.toSummary(user, now));
    }

    @Transactional(readOnly = true)
    public AdminUserDetailResponse getUser(String username) {
        User user =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(AdminUserQueryService::userNotFound);
        return AdminUserViews.toDetail(user, clock.instant());
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

    private static NotFoundException userNotFound() {
        return new NotFoundException(ErrorCode.USER_NOT_FOUND, "Không tìm thấy người dùng");
    }
}
