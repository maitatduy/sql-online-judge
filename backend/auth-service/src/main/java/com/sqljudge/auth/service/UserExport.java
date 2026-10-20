package com.sqljudge.auth.service;

import com.sqljudge.auth.dto.request.ExportScope;
import com.sqljudge.auth.dto.request.ExportUsersRequest;
import com.sqljudge.auth.entity.User;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

public record UserExport(ExportUsersRequest request, Instant now, List<User> selectedUsers) {

    private static final DateTimeFormatter FILE_STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmm").withZone(ZoneOffset.UTC);

    public UserExport {
        selectedUsers = List.copyOf(selectedUsers);
    }

    public String fileName() {
        return "users-" + FILE_STAMP.format(now) + ".csv";
    }

    boolean isSelection() {
        return request.scope() == ExportScope.SELECTED;
    }
}
