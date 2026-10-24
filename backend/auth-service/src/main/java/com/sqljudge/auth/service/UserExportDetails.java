package com.sqljudge.auth.service;

import com.sqljudge.auth.dto.request.ExportScope;
import com.sqljudge.auth.dto.request.ExportUsersRequest;
import com.sqljudge.auth.dto.request.UserExportFilters;
import java.util.ArrayList;
import java.util.List;

public final class UserExportDetails {

    private static final String HIDDEN_KEYWORD = "*";

    private UserExportDetails() {}

    public static String of(ExportUsersRequest request, long rows) {
        List<String> parts = new ArrayList<>();
        parts.add("scope=" + request.scope());
        parts.add("rows=" + rows);
        if (request.scope() == ExportScope.FILTERED) {
            parts.addAll(filterParts(request.filters()));
        }
        return String.join(";", parts);
    }

    private static List<String> filterParts(UserExportFilters filters) {
        List<String> parts = new ArrayList<>();
        if (filters.q() != null && !filters.q().isBlank()) {
            parts.add("q=" + HIDDEN_KEYWORD);
        }
        addIfPresent(parts, "role", filters.role());
        addIfPresent(parts, "status", filters.status());
        addIfPresent(parts, "emailVerified", filters.emailVerified());
        addIfPresent(parts, "deleted", filters.deleted());
        return parts;
    }

    private static void addIfPresent(List<String> parts, String name, Object value) {
        if (value != null) {
            parts.add(name + "=" + value);
        }
    }
}
