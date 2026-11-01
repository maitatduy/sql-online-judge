package com.sqljudge.judge.sandbox;

import com.sqljudge.judge.compare.ExecutionOutcome;
import com.sqljudge.judge.compare.ExecutionOutcome.Failed;
import com.sqljudge.judge.compare.ExecutionOutcome.SandboxFailed;
import com.sqljudge.judge.compare.ExecutionOutcome.Succeeded;
import com.sqljudge.judge.compare.ExecutionOutcome.TimedOut;
import com.sqljudge.judge.compare.ResultTable;
import com.sqljudge.judge.compare.SanitizedMessage;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import java.util.regex.Pattern;

public abstract class JdbcSandbox implements Sandbox {

    protected static final Pattern RUN_NAME = Pattern.compile("sb_[0-9a-f]{28}");
    private static final int RUN_NAME_HEX_LENGTH = 28;

    @Override
    public final ExecutionOutcome run(QueryRun run) {
        String runName = "sb_" + randomHex().substring(0, RUN_NAME_HEX_LENGTH);
        try (Connection admin = connectAdmin(run.limits().timeout())) {
            try {
                String password = randomHex();
                prepare(admin, runName, password, run);
                return execute(runName, password, run);
            } finally {
                cleanUp(admin, runName);
            }
        } catch (SQLException exception) {
            return new SandboxFailed("sandbox_unavailable", 0);
        } catch (IllegalArgumentException | DateTimeParseException exception) {
            return new SandboxFailed("invalid_dataset_snapshot", 0);
        }
    }

    protected abstract Connection connectAdmin(Duration timeout) throws SQLException;

    protected abstract void prepare(Connection admin, String runName, String password, QueryRun run)
            throws SQLException;

    protected abstract Connection connectRunner(String runName, String password, Duration timeout)
            throws SQLException;

    protected abstract void cleanUp(Connection admin, String runName) throws SQLException;

    protected abstract boolean isTimeout(SQLException exception);

    protected abstract String messageOf(SQLException exception);

    protected static void loadDataset(
            Connection admin, String namespace, DatasetSnapshot dataset, DatasetDdl ddl)
            throws SQLException {
        try (Statement statement = admin.createStatement()) {
            for (SnapshotTable table : dataset.tables()) {
                statement.execute(
                        ddl.createTable(namespace, table, DatasetDdl.keyColumnsOf(dataset, table)));
                SnapshotLoader.insertRows(admin, ddl.insert(namespace, table), table);
            }
            for (SnapshotTable table : dataset.tables()) {
                for (ForeignKey foreignKey : table.foreignKeys()) {
                    statement.execute(ddl.addForeignKey(namespace, table.name(), foreignKey));
                }
            }
        }
    }

    protected static int timeoutSeconds(Duration timeout) {
        return (int) Math.max(1, (timeout.toMillis() + 999) / 1000);
    }

    private ExecutionOutcome execute(String runName, String password, QueryRun run) {
        RunLimits limits = run.limits();
        long startedAt = System.nanoTime();
        try (Connection runner = connectRunner(runName, password, limits.timeout());
                Statement statement = runner.createStatement()) {
            statement.setQueryTimeout(timeoutSeconds(limits.timeout()));
            statement.setMaxRows(limits.maxRows() + 1);
            if (!statement.execute(run.sql())) {
                return failed("Câu lệnh không trả về bảng kết quả", startedAt);
            }
            ResultTable table;
            try (ResultSet resultSet = statement.getResultSet()) {
                table = JdbcResultReader.read(resultSet, limits);
            }
            if (statement.getMoreResults() || statement.getUpdateCount() != -1) {
                return failed("Bài này chỉ cho phép 1 câu lệnh", startedAt);
            }
            return new Succeeded(table, elapsedMs(startedAt));
        } catch (ResultLimitExceededException exception) {
            return failed(exception.getMessage(), startedAt);
        } catch (SQLException exception) {
            if (isTimeout(exception)) {
                return new TimedOut(elapsedMs(startedAt));
            }
            return failed(withoutRunName(messageOf(exception)), startedAt);
        }
    }

    private static String withoutRunName(String message) {
        return RUN_NAME.matcher(String.valueOf(message))
                .replaceAll("")
                .replaceAll(" {2,}", " ")
                .trim();
    }

    private static Failed failed(String message, long startedAt) {
        return new Failed(new SanitizedMessage(message), elapsedMs(startedAt));
    }

    private static long elapsedMs(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }

    private static String randomHex() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
