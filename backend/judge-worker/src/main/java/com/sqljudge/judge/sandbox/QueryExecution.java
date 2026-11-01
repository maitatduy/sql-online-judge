package com.sqljudge.judge.sandbox;

import com.sqljudge.judge.compare.ExecutionOutcome;
import com.sqljudge.judge.compare.ExecutionOutcome.Failed;
import com.sqljudge.judge.compare.ExecutionOutcome.Succeeded;
import com.sqljudge.judge.compare.ExecutionOutcome.TimedOut;
import com.sqljudge.judge.compare.ResultTable;
import com.sqljudge.judge.compare.SanitizedMessage;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.regex.Pattern;

final class QueryExecution {

    static final Pattern RUN_NAME = Pattern.compile("sb_[0-9a-f]{28}");

    @FunctionalInterface
    interface RunnerConnection {
        Connection open() throws SQLException;
    }

    private QueryExecution() {}

    static ExecutionOutcome execute(RunnerConnection runner, QueryRun run, SqlErrors errors) {
        RunLimits limits = run.limits();
        long startedAt = System.nanoTime();
        try (Connection connection = runner.open();
                Statement statement = connection.createStatement()) {
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
            if (errors.isTimeout(exception)) {
                return new TimedOut(elapsedMs(startedAt));
            }
            return failed(withoutRunName(errors.messageOf(exception)), startedAt);
        }
    }

    static int timeoutSeconds(Duration timeout) {
        return (int) Math.max(1, (timeout.toMillis() + 999) / 1000);
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
}
