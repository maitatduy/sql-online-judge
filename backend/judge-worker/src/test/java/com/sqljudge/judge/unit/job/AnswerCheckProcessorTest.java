package com.sqljudge.judge.unit.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.sqljudge.judge.client.DatasetSnapshotResponse;
import com.sqljudge.judge.client.DatasetSnapshotResponse.SnapshotColumn;
import com.sqljudge.judge.client.DatasetSnapshotResponse.SnapshotTable;
import com.sqljudge.judge.client.DatasetSnapshotResponse.SnapshotVariant;
import com.sqljudge.judge.client.SnapshotSource;
import com.sqljudge.judge.client.SnapshotUnavailableException;
import com.sqljudge.judge.compare.CompareOptions;
import com.sqljudge.judge.compare.ExecutionOutcome;
import com.sqljudge.judge.compare.MismatchReason;
import com.sqljudge.judge.compare.ResultTable;
import com.sqljudge.judge.compare.SanitizedMessage;
import com.sqljudge.judge.compare.Verdict;
import com.sqljudge.judge.guard.StatementGuard;
import com.sqljudge.judge.guard.StatementType;
import com.sqljudge.judge.job.AnswerCheckProcessor;
import com.sqljudge.judge.job.AnswerCheckRequest;
import com.sqljudge.judge.job.AnswerCheckTestcase;
import com.sqljudge.judge.job.DialectCheck;
import com.sqljudge.judge.job.ExpectedTable.CellKind;
import com.sqljudge.judge.job.ExpectedTable.WireCell;
import com.sqljudge.judge.job.TestcaseCheckResult;
import com.sqljudge.judge.sandbox.Dialect;
import com.sqljudge.judge.sandbox.LogicalType;
import com.sqljudge.judge.sandbox.QueryRun;
import com.sqljudge.judge.sandbox.Sandbox;
import com.sqljudge.judge.sandbox.SandboxRegistry;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AnswerCheckProcessorTest {

    private static final UUID CHECK_ID = UUID.randomUUID();
    private static final UUID VERSION_ID = UUID.randomUUID();
    private static final UUID SAMPLE_VARIANT = UUID.randomUUID();
    private static final UUID HIDDEN_VARIANT = UUID.randomUUID();
    private static final UUID SAMPLE_TESTCASE = UUID.randomUUID();
    private static final UUID HIDDEN_TESTCASE = UUID.randomUUID();
    private static final String CHECKSUM = "abc123";
    private static final String SOLUTION = "SELECT id, name, opened_on FROM departments";
    private static final DatasetSnapshotResponse SNAPSHOT =
            new DatasetSnapshotResponse(
                    VERSION_ID,
                    CHECKSUM,
                    List.of(
                            new SnapshotTable(
                                    "departments",
                                    List.of(
                                            new SnapshotColumn(
                                                    "id",
                                                    LogicalType.INTEGER,
                                                    true,
                                                    false,
                                                    null)))),
                    List.of(
                            new SnapshotVariant(SAMPLE_VARIANT, "Mẫu", true, List.of()),
                            new SnapshotVariant(HIDDEN_VARIANT, "Ẩn", false, List.of())));

    private final Map<Dialect, List<QueryRun>> runs = new EnumMap<>(Dialect.class);
    private final List<TestcaseCheckResult> published = new ArrayList<>();

    @Test
    @DisplayName("Mọi dialect ra cùng kết quả thì từng testcase có bảng mong đợi và AC")
    void process_allDialectsAgree_publishesExpectedTablePerTestcase() {
        Map<Dialect, Sandbox> sandboxes = new EnumMap<>(Dialect.class);
        sandboxes.put(
                Dialect.POSTGRESQL, succeedsWith(row(1, "Kỹ thuật", LocalDate.of(2024, 3, 1))));
        sandboxes.put(Dialect.SQLITE, succeedsWith(row(1, "Kỹ thuật", "2024-03-01")));

        process(sandboxes, request(solutions(Dialect.SQLITE, Dialect.POSTGRESQL), false));

        assertThat(published)
                .extracting(TestcaseCheckResult::testcaseId)
                .containsExactly(SAMPLE_TESTCASE, HIDDEN_TESTCASE);
        TestcaseCheckResult first = published.get(0);
        assertThat(first.checkId()).isEqualTo(CHECK_ID);
        assertThat(first.position()).isEqualTo(1);
        assertThat(first.testcaseCount()).isEqualTo(2);
        assertThat(first.systemError()).isNull();
        assertThat(first.expected().columns()).containsExactly("id", "name", "opened_on");
        assertThat(first.expected().rows())
                .containsExactly(
                        List.of(
                                new WireCell(CellKind.NUMBER, "1"),
                                new WireCell(CellKind.TEXT, "Kỹ thuật"),
                                new WireCell(CellKind.DATE, "2024-03-01")));
        assertThat(first.dialects())
                .extracting(DialectCheck::dialect, DialectCheck::verdict, DialectCheck::rowCount)
                .containsExactly(
                        tuple(Dialect.POSTGRESQL, Verdict.AC, 1),
                        tuple(Dialect.SQLITE, Verdict.AC, 1));
        assertThat(runs.get(Dialect.POSTGRESQL))
                .extracting(run -> run.limits().timeout().toMillis())
                .containsOnly(3000L);
    }

    @Test
    @DisplayName("Dialect lệch kết quả với PostgreSQL thì ghi WA kèm lý do, OM khi chỉ sai thứ tự")
    void process_dialectDisagrees_reportsMismatchAgainstReference() {
        Map<Dialect, Sandbox> sandboxes = new EnumMap<>(Dialect.class);
        sandboxes.put(Dialect.POSTGRESQL, succeedsWith(row(1, "A", null), row(2, "B", null)));
        sandboxes.put(Dialect.MYSQL, succeedsWith(row(2, "B", null), row(1, "A", null)));
        sandboxes.put(Dialect.SQLITE, succeedsWith(row(1, "A", null)));

        process(
                sandboxes,
                request(solutions(Dialect.POSTGRESQL, Dialect.MYSQL, Dialect.SQLITE), true));

        List<DialectCheck> dialects = published.get(0).dialects();
        assertThat(dialects)
                .extracting(DialectCheck::dialect, DialectCheck::verdict, DialectCheck::mismatch)
                .containsExactly(
                        tuple(Dialect.POSTGRESQL, Verdict.AC, MismatchReason.NONE),
                        tuple(Dialect.MYSQL, Verdict.OM, MismatchReason.ORDER),
                        tuple(Dialect.SQLITE, Verdict.WA, MismatchReason.ROWS));
        assertThat(published.get(0).expected().rows().get(0).get(2))
                .isEqualTo(new WireCell(CellKind.NULL, null));
    }

    @Test
    @DisplayName("Đáp án bị guard chặn thì dialect đó ra FS ở mọi testcase và không được chạy")
    void process_rejectedSolution_isNotRun() {
        Map<Dialect, Sandbox> sandboxes = new EnumMap<>(Dialect.class);
        sandboxes.put(Dialect.POSTGRESQL, succeedsWith(row(1, "A", null)));
        sandboxes.put(Dialect.MYSQL, succeedsWith(row(1, "A", null)));
        Map<Dialect, String> solutions = solutions(Dialect.POSTGRESQL);
        solutions.put(Dialect.MYSQL, "DELETE FROM departments");

        process(sandboxes, request(solutions, false));

        assertThat(published)
                .allSatisfy(
                        result -> {
                            DialectCheck mysql = result.dialects().get(1);
                            assertThat(mysql.verdict()).isEqualTo(Verdict.FS);
                            assertThat(mysql.message())
                                    .isEqualTo("Bài này chỉ cho phép SELECT, WITH");
                        });
        assertThat(runs.get(Dialect.MYSQL)).isNull();
        assertThat(runs.get(Dialect.POSTGRESQL)).hasSize(2);
    }

    @Test
    @DisplayName("Đáp án tham chiếu lỗi thì không có bảng mong đợi và dialect khác không được so")
    void process_referenceFails_leavesOthersUncompared() {
        Map<Dialect, Sandbox> sandboxes = new EnumMap<>(Dialect.class);
        sandboxes.put(
                Dialect.POSTGRESQL,
                run -> new ExecutionOutcome.Failed(new SanitizedMessage("syntax error"), 2));
        sandboxes.put(Dialect.SQLITE, succeedsWith(row(1, "A", null)));

        process(sandboxes, request(solutions(Dialect.POSTGRESQL, Dialect.SQLITE), false));

        TestcaseCheckResult first = published.get(0);
        assertThat(first.expected()).isNull();
        assertThat(first.dialects())
                .extracting(DialectCheck::verdict, DialectCheck::message)
                .containsExactly(tuple(Verdict.RE, "syntax error"), tuple(null, null));
    }

    @Test
    @DisplayName("Quá giờ, lỗi sandbox và thiếu sandbox được ghi theo từng dialect")
    void process_executionProblems_areReportedPerDialect() {
        Map<Dialect, Sandbox> sandboxes = new EnumMap<>(Dialect.class);
        sandboxes.put(Dialect.POSTGRESQL, run -> new ExecutionOutcome.TimedOut(3000));
        sandboxes.put(Dialect.MYSQL, run -> new ExecutionOutcome.SandboxFailed("lost", 1));

        process(
                sandboxes,
                request(solutions(Dialect.POSTGRESQL, Dialect.MYSQL, Dialect.SQLITE), false));

        assertThat(published.get(0).dialects())
                .extracting(DialectCheck::verdict, DialectCheck::runtimeMs)
                .containsExactly(
                        tuple(Verdict.TLE, 3000L), tuple(Verdict.SE, 1L), tuple(Verdict.SE, 0L));
    }

    @Test
    @DisplayName("Không tải được dataset thì từng testcase báo lỗi hệ thống")
    void process_snapshotUnavailable_reportsEveryTestcase() {
        SnapshotSource failing =
                (versionId, checksum) -> {
                    throw new SnapshotUnavailableException("problem-service down", null);
                };
        AnswerCheckProcessor processor =
                new AnswerCheckProcessor(
                        new StatementGuard(), failing, new SandboxRegistry(Map.of()));

        processor.process(request(solutions(Dialect.POSTGRESQL), false), published::add);

        assertThat(published)
                .extracting(TestcaseCheckResult::systemError)
                .containsExactly("snapshot_unavailable", "snapshot_unavailable");
        assertThat(published).allSatisfy(result -> assertThat(result.dialects()).isEmpty());
    }

    @Test
    @DisplayName("Testcase trỏ tới bộ dữ liệu không còn trong snapshot thì báo lỗi hệ thống")
    void process_missingVariant_reportsSystemError() {
        AnswerCheckTestcase orphan = new AnswerCheckTestcase(UUID.randomUUID(), UUID.randomUUID());
        AnswerCheckRequest request =
                new AnswerCheckRequest(
                        CHECK_ID,
                        VERSION_ID,
                        CHECKSUM,
                        solutions(Dialect.POSTGRESQL),
                        List.of(orphan),
                        new CompareOptions(false, false, CompareOptions.DEFAULT_TOLERANCE),
                        3000,
                        Set.of(StatementType.SELECT, StatementType.WITH));

        process(Map.of(Dialect.POSTGRESQL, succeedsWith(row(1, "A", null))), request);

        assertThat(published.get(0).systemError()).isEqualTo("variant_missing");
        assertThat(runs).isEmpty();
    }

    private void process(Map<Dialect, Sandbox> sandboxes, AnswerCheckRequest request) {
        Map<Dialect, Sandbox> recording = new EnumMap<>(Dialect.class);
        sandboxes.forEach(
                (dialect, sandbox) ->
                        recording.put(
                                dialect,
                                run -> {
                                    runs.computeIfAbsent(dialect, key -> new ArrayList<>())
                                            .add(run);
                                    return sandbox.run(run);
                                }));
        AnswerCheckProcessor processor =
                new AnswerCheckProcessor(
                        new StatementGuard(),
                        (versionId, checksum) -> SNAPSHOT,
                        new SandboxRegistry(recording));
        processor.process(request, published::add);
    }

    private static AnswerCheckRequest request(Map<Dialect, String> solutions, boolean ordered) {
        return new AnswerCheckRequest(
                CHECK_ID,
                VERSION_ID,
                CHECKSUM,
                solutions,
                List.of(
                        new AnswerCheckTestcase(SAMPLE_TESTCASE, SAMPLE_VARIANT),
                        new AnswerCheckTestcase(HIDDEN_TESTCASE, HIDDEN_VARIANT)),
                new CompareOptions(ordered, false, CompareOptions.DEFAULT_TOLERANCE),
                3000,
                Set.of(StatementType.SELECT, StatementType.WITH));
    }

    private static Map<Dialect, String> solutions(Dialect... dialects) {
        Map<Dialect, String> solutions = new LinkedHashMap<>();
        for (Dialect dialect : dialects) {
            solutions.put(dialect, SOLUTION);
        }
        return solutions;
    }

    private static List<Object> row(Object... values) {
        return Arrays.asList(values);
    }

    @SafeVarargs
    private static Sandbox succeedsWith(List<Object>... rows) {
        return run ->
                new ExecutionOutcome.Succeeded(
                        new ResultTable(List.of("id", "name", "opened_on"), List.of(rows)), 4);
    }
}
