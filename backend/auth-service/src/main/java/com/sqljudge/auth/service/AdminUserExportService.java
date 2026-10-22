package com.sqljudge.auth.service;

import com.sqljudge.auth.config.AdminProperties;
import com.sqljudge.auth.dto.request.ExportScope;
import com.sqljudge.auth.dto.request.ExportUsersRequest;
import com.sqljudge.auth.dto.request.UserExportFilters;
import com.sqljudge.auth.entity.AdminAction;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.repository.AdminActionRepository;
import com.sqljudge.auth.repository.UserRepository;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserExportService {

    private static final AdminUserSearch ALL_LIVE_USERS =
            new AdminUserSearch(null, null, null, null, false);

    private final UserRepository userRepository;
    private final AdminActionRepository adminActionRepository;
    private final AdminProperties adminProperties;

    @Transactional
    public UserExport prepare(ExportUsersRequest request, UUID actorId, Instant now) {
        List<User> selectedUsers =
                request.scope() == ExportScope.SELECTED
                        ? selectedUsers(request.usernames())
                        : List.of();
        UserExport export = new UserExport(request, now, selectedUsers);
        if (request.isEmailIncluded()) {
            audit(export, actorId);
        }
        log.info(
                "admin_users_export_started actorId={} scope={} includeEmail={}",
                actorId,
                request.scope(),
                request.isEmailIncluded());
        return export;
    }

    public void write(UserExport export, OutputStream out) throws IOException {
        try {
            writeCsv(export, out);
        } catch (IOException | RuntimeException ex) {
            log.warn("admin_users_export_failed scope={}", export.request().scope(), ex);
            throw ex;
        }
    }

    private void audit(UserExport export, UUID actorId) {
        long rows =
                export.isSelection()
                        ? export.selectedUsers().size()
                        : userRepository.count(matching(export));
        adminActionRepository.save(
                AdminAction.exportUserEmails(
                        actorId,
                        export.request().reason().strip(),
                        UserExportDetails.of(export.request(), rows)));
    }

    private void writeCsv(UserExport export, OutputStream out) throws IOException {
        boolean includeEmail = export.request().isEmailIncluded();
        Function<User, String> toRow = user -> UserCsv.row(user, includeEmail, export.now());
        Writer writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        writer.write(UserCsv.header(includeEmail));
        if (export.isSelection()) {
            writeRows(writer, export.selectedUsers(), toRow);
        } else {
            writeMatchingUsers(writer, matching(export), toRow);
        }
        writer.flush();
    }

    private void writeMatchingUsers(
            Writer writer, Specification<User> matching, Function<User, String> toRow)
            throws IOException {
        List<User> batch = nextBatch(matching);
        while (!batch.isEmpty()) {
            writeRows(writer, batch, toRow);
            writer.flush();
            if (batch.size() < batchSize()) {
                return;
            }
            User last = batch.get(batch.size() - 1);
            batch = nextBatch(matching.and(AdminUserSpecifications.olderThan(last)));
        }
    }

    private static void writeRows(Writer writer, List<User> users, Function<User, String> toRow)
            throws IOException {
        for (User user : users) {
            writer.write(toRow.apply(user));
        }
    }

    private List<User> nextBatch(Specification<User> specification) {
        return userRepository.findBy(
                specification,
                query ->
                        query.sortBy(AdminUserSpecifications.NEWEST_FIRST)
                                .limit(batchSize())
                                .all());
    }

    private int batchSize() {
        return adminProperties.userExport().batchSize();
    }

    private List<User> selectedUsers(List<String> requestedUsernames) {
        Set<String> usernames = new LinkedHashSet<>(requestedUsernames);
        Map<String, User> usersByName =
                userRepository.findAllByUsernameIn(usernames).stream()
                        .collect(Collectors.toMap(User::getUsername, Function.identity()));
        return usernames.stream().map(usersByName::get).filter(Objects::nonNull).toList();
    }

    private static Specification<User> matching(UserExport export) {
        ExportUsersRequest request = export.request();
        AdminUserSearch search =
                request.scope() == ExportScope.FILTERED
                        ? searchOf(request.filters())
                        : ALL_LIVE_USERS;
        return AdminUserSpecifications.matching(search, export.now())
                .and(AdminUserSpecifications.createdNoLaterThan(export.now()));
    }

    private static AdminUserSearch searchOf(UserExportFilters filters) {
        return new AdminUserSearch(
                filters.q(),
                filters.role(),
                filters.status(),
                filters.emailVerified(),
                Boolean.TRUE.equals(filters.deleted()));
    }
}
