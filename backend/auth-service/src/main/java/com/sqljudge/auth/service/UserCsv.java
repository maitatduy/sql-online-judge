package com.sqljudge.auth.service;

import com.sqljudge.auth.dto.response.AccountStatus;
import com.sqljudge.auth.entity.User;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class UserCsv {

    private UserCsv() {}

    public static String header(boolean includeEmail) {
        List<String> columns = new ArrayList<>(List.of("username"));
        if (includeEmail) {
            columns.add("email");
        }
        columns.addAll(List.of("role", "status", "joinedAt"));
        return CsvLines.line(columns);
    }

    public static String row(User user, boolean includeEmail, Instant now) {
        List<String> cells = new ArrayList<>(List.of(user.getUsername()));
        if (includeEmail) {
            cells.add(user.getEmail());
        }
        cells.add(user.getRole().name());
        cells.add((user.isBannedAt(now) ? AccountStatus.BANNED : AccountStatus.ACTIVE).name());
        cells.add(user.getCreatedAt().toString());
        return CsvLines.line(cells);
    }
}
