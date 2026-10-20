package com.sqljudge.auth.service;

import com.sqljudge.auth.dto.response.AccountStatus;
import com.sqljudge.auth.entity.User;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class UserCsv {

    private static final String SEPARATOR = ",";
    private static final String LINE_END = "\n";
    private static final String FORMULA_GUARD = "'";
    private static final String FORMULA_PREFIXES = "=+-@\t\r";
    private static final String QUOTE = "\"";

    private UserCsv() {}

    public static String header(boolean includeEmail) {
        List<String> columns = new ArrayList<>(List.of("username"));
        if (includeEmail) {
            columns.add("email");
        }
        columns.addAll(List.of("role", "status", "joinedAt"));
        return line(columns);
    }

    public static String row(User user, boolean includeEmail, Instant now) {
        List<String> cells = new ArrayList<>(List.of(user.getUsername()));
        if (includeEmail) {
            cells.add(user.getEmail());
        }
        cells.add(user.getRole().name());
        cells.add((user.isBannedAt(now) ? AccountStatus.BANNED : AccountStatus.ACTIVE).name());
        cells.add(user.getCreatedAt().toString());
        return line(cells);
    }

    private static String line(List<String> cells) {
        return String.join(SEPARATOR, cells.stream().map(UserCsv::cell).toList()) + LINE_END;
    }

    private static String cell(String value) {
        String guarded = startsWithFormula(value) ? FORMULA_GUARD + value : value;
        if (!needsQuoting(guarded)) {
            return guarded;
        }
        return QUOTE + guarded.replace(QUOTE, QUOTE + QUOTE) + QUOTE;
    }

    private static boolean startsWithFormula(String value) {
        return !value.isEmpty() && FORMULA_PREFIXES.indexOf(value.charAt(0)) >= 0;
    }

    private static boolean needsQuoting(String value) {
        return value.contains(SEPARATOR)
                || value.contains(QUOTE)
                || value.contains("\n")
                || value.contains("\r");
    }
}
