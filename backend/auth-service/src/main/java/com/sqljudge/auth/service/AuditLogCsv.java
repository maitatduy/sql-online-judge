package com.sqljudge.auth.service;

import com.sqljudge.auth.dto.response.AuditLogEntryResponse;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class AuditLogCsv {

    private static final DateTimeFormatter FILE_STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmm").withZone(ZoneOffset.UTC);
    private static final List<String> COLUMNS =
            List.of(
                    "occurredAt",
                    "actor",
                    "actorRole",
                    "action",
                    "target",
                    "previousValue",
                    "newValue",
                    "reason",
                    "expiresAt",
                    "details");

    private AuditLogCsv() {}

    public static String header() {
        return CsvLines.line(COLUMNS);
    }

    public static String row(AuditLogEntryResponse entry) {
        return CsvLines.line(
                cells(
                        entry.occurredAt(),
                        entry.actorUsername(),
                        entry.actorRole(),
                        entry.action(),
                        entry.targetUsername(),
                        entry.previousValue(),
                        entry.newValue(),
                        entry.reason(),
                        entry.expiresAt(),
                        entry.details()));
    }

    public static String fileName(Instant now) {
        return "audit-log-" + FILE_STAMP.format(now) + ".csv";
    }

    private static List<String> cells(Object... values) {
        return Arrays.stream(values).map(value -> Objects.toString(value, "")).toList();
    }
}
