package com.sqljudge.auth.service;

import com.sqljudge.auth.dto.request.BanUserRequest;
import com.sqljudge.auth.dto.request.BulkBanUsersRequest;
import com.sqljudge.auth.dto.request.BulkChangeRoleRequest;
import com.sqljudge.auth.dto.response.BulkUserActionResponse;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.common.security.Role;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserBulkService {

    private final UserRepository userRepository;
    private final AdminUserBanService adminUserBanService;
    private final AdminUserEditService adminUserEditService;
    private final Clock clock;

    @Transactional
    public BulkUserActionResponse changeRole(BulkChangeRoleRequest request, UUID actorId) {
        RoleChange change = new RoleChange(request.role(), request.reason().strip());
        BulkUserActionResponse response =
                applyToEligible(
                        request.usernames(),
                        actorId,
                        new BulkChange(
                                user -> user.getRole() != request.role(),
                                user ->
                                        adminUserEditService.applyRoleChange(
                                                user, change, actorId)));
        log.info(
                "admin_users_bulk_role_changed actorId={} role={} affected={} skipped={}",
                actorId,
                request.role(),
                response.affectedCount(),
                response.skippedUsernames().size());
        return response;
    }

    @Transactional
    public BulkUserActionResponse ban(BulkBanUsersRequest request, UUID actorId) {
        Instant now = clock.instant();
        BanUserRequest banRequest = request.toBanRequest();
        BulkUserActionResponse response =
                applyToEligible(
                        request.usernames(),
                        actorId,
                        new BulkChange(
                                user -> !user.isBannedAt(now),
                                user -> adminUserBanService.applyBan(user, banRequest, actorId)));
        log.info(
                "admin_users_bulk_banned actorId={} duration={} affected={} skipped={}",
                actorId,
                request.duration(),
                response.affectedCount(),
                response.skippedUsernames().size());
        return response;
    }

    private BulkUserActionResponse applyToEligible(
            List<String> requestedUsernames, UUID actorId, BulkChange change) {
        Set<String> usernames = new LinkedHashSet<>(requestedUsernames);
        Map<String, User> usersByName =
                userRepository.findAllByUsernameIn(usernames).stream()
                        .collect(Collectors.toMap(User::getUsername, Function.identity()));
        List<String> skipped = new ArrayList<>();
        for (String username : usernames) {
            User user = usersByName.get(username);
            if (user == null || !isBulkTarget(user, actorId) || !change.isNeeded().test(user)) {
                skipped.add(username);
                continue;
            }
            change.apply().accept(user);
        }
        return new BulkUserActionResponse(usernames.size() - skipped.size(), skipped);
    }

    private static boolean isBulkTarget(User user, UUID actorId) {
        return !user.isDeleted() && !user.getId().equals(actorId) && user.getRole() != Role.ADMIN;
    }

    private record BulkChange(Predicate<User> isNeeded, Consumer<User> apply) {}
}
