package com.sqljudge.judge.integration.sandbox;

import com.sqljudge.judge.sandbox.DatasetSnapshot;
import com.sqljudge.judge.sandbox.ForeignKey;
import com.sqljudge.judge.sandbox.LogicalType;
import com.sqljudge.judge.sandbox.RunLimits;
import com.sqljudge.judge.sandbox.SnapshotColumn;
import com.sqljudge.judge.sandbox.SnapshotTable;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

final class SandboxFixtures {

    static final RunLimits FAST_LIMITS = new RunLimits(Duration.ofSeconds(1), 100, 64_000);

    private SandboxFixtures() {}

    static DatasetSnapshot company() {
        return new DatasetSnapshot(
                List.of(
                        new SnapshotTable(
                                "departments",
                                List.of(key("id"), column("name", LogicalType.TEXT, false)),
                                List.of(),
                                List.of(row("1", "Kỹ thuật"), row("2", "Kinh doanh"))),
                        new SnapshotTable(
                                "employees",
                                List.of(
                                        key("id"),
                                        column("full_name", LogicalType.TEXT, false),
                                        column("salary", LogicalType.DECIMAL, false),
                                        column("department_id", LogicalType.INTEGER, true),
                                        column("hired_at", LogicalType.DATE, false),
                                        column("is_active", LogicalType.BOOLEAN, false),
                                        column("updated_at", LogicalType.TIMESTAMP, true)),
                                List.of(new ForeignKey("department_id", "departments", "id")),
                                List.of(
                                        row(
                                                "1",
                                                "Hoàng Quốc Bảo",
                                                "48000000.50",
                                                "1",
                                                "2024-03-01",
                                                "true",
                                                "2026-01-05 09:30:00"),
                                        row(
                                                "2",
                                                "Mai Lan",
                                                "30000000",
                                                null,
                                                "2025-07-15",
                                                "0",
                                                null)))));
    }

    static DatasetSnapshot textKeyed() {
        return new DatasetSnapshot(
                List.of(
                        new SnapshotTable(
                                "regions",
                                List.of(
                                        new SnapshotColumn("code", LogicalType.TEXT, true, false),
                                        column("name", LogicalType.TEXT, false)),
                                List.of(),
                                List.of(row("HN", "Hà Nội"))),
                        new SnapshotTable(
                                "cities",
                                List.of(key("id"), column("region_code", LogicalType.TEXT, false)),
                                List.of(new ForeignKey("region_code", "regions", "code")),
                                List.of(row("1", "HN")))));
    }

    private static SnapshotColumn key(String name) {
        return new SnapshotColumn(name, LogicalType.INTEGER, true, false);
    }

    private static SnapshotColumn column(String name, LogicalType type, boolean isNullable) {
        return new SnapshotColumn(name, type, false, isNullable);
    }

    private static List<String> row(String... values) {
        return Arrays.asList(values);
    }
}
