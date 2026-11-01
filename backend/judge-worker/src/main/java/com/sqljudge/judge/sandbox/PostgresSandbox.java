package com.sqljudge.judge.sandbox;

import com.sqljudge.judge.compare.ExecutionOutcome;
import com.sqljudge.judge.compare.ExecutionOutcome.Failed;
import com.sqljudge.judge.compare.ExecutionOutcome.SandboxFailed;
import com.sqljudge.judge.compare.ExecutionOutcome.Succeeded;
import com.sqljudge.judge.compare.ExecutionOutcome.TimedOut;
import com.sqljudge.judge.compare.ResultTable;
import com.sqljudge.judge.compare.SanitizedMessage;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.Statement;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Pattern;
import org.postgresql.util.PSQLException;

public final class PostgresSandbox implements Sandbox {

    private static final String QUERY_CANCELED = "57014";
    private static final Pattern RUN_SCHEMA = Pattern.compile("sb_[0-9a-f]{32}\\.?");
    private static final int CONNECT_TIMEOUT_SECONDS = 5;
    private static final int SOCKET_TIMEOUT_MARGIN_SECONDS = 5;

    private final PostgresSandboxConfig config;

    public PostgresSandbox(PostgresSandboxConfig config) {
        this.config = config;
    }

    @Override
    public ExecutionOutcome run(QueryRun run) {
        String runName = "sb_" + randomHex();
        try (Connection admin =
                connect(config.adminUser(), config.adminPassword(), run.limits().timeout())) {
            try {
                String password = prepare(admin, runName, run);
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

    private String prepare(Connection admin, String runName, QueryRun run) throws SQLException {
        String password = randomHex();
        try (Statement statement = admin.createStatement()) {
            statement.execute("CREATE SCHEMA " + runName);
            for (SnapshotTable table : run.dataset().tables()) {
                statement.execute(PostgresDdl.createTable(runName, table));
                SnapshotLoader.insertRows(admin, PostgresDdl.insert(runName, table), table);
            }
            for (SnapshotTable table : run.dataset().tables()) {
                for (ForeignKey foreignKey : table.foreignKeys()) {
                    statement.execute(PostgresDdl.addForeignKey(runName, table.name(), foreignKey));
                }
            }
            statement.execute(
                    "CREATE ROLE "
                            + runName
                            + " LOGIN PASSWORD '"
                            + password
                            + "' NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION"
                            + " CONNECTION LIMIT 1");
            statement.execute("GRANT USAGE ON SCHEMA " + runName + " TO " + runName);
            statement.execute("GRANT SELECT ON ALL TABLES IN SCHEMA " + runName + " TO " + runName);
            statement.execute("ALTER ROLE " + runName + " SET search_path = " + runName);
            statement.execute("ALTER ROLE " + runName + " SET default_transaction_read_only = on");
            statement.execute(
                    "ALTER ROLE "
                            + runName
                            + " SET statement_timeout = "
                            + run.limits().timeout().toMillis());
        }
        return password;
    }

    private ExecutionOutcome execute(String runName, String password, QueryRun run) {
        RunLimits limits = run.limits();
        long startedAt = System.nanoTime();
        try (Connection runner = connect(runName, password, limits.timeout());
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
            return failed(sanitizedMessageOf(exception), startedAt);
        }
    }

    private void cleanUp(Connection admin, String runName) throws SQLException {
        try (PreparedStatement terminate =
                admin.prepareStatement(
                        "SELECT pg_terminate_backend(pid) FROM pg_stat_activity"
                                + " WHERE usename = ?")) {
            terminate.setString(1, runName);
            terminate.execute();
        }
        try (Statement statement = admin.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + runName + " CASCADE");
            statement.execute("DROP ROLE IF EXISTS " + runName);
        }
    }

    private Connection connect(String user, String password, Duration timeout) throws SQLException {
        Properties properties = new Properties();
        properties.setProperty("user", user);
        properties.setProperty("password", password);
        properties.setProperty("connectTimeout", String.valueOf(CONNECT_TIMEOUT_SECONDS));
        properties.setProperty(
                "socketTimeout",
                String.valueOf(timeoutSeconds(timeout) + SOCKET_TIMEOUT_MARGIN_SECONDS));
        properties.setProperty("ApplicationName", "sqljudge-sandbox");
        return DriverManager.getConnection(config.jdbcUrl(), properties);
    }

    private static boolean isTimeout(SQLException exception) {
        return exception instanceof SQLTimeoutException
                || QUERY_CANCELED.equals(exception.getSQLState());
    }

    private static String sanitizedMessageOf(SQLException exception) {
        String message =
                exception instanceof PSQLException postgres
                                && postgres.getServerErrorMessage() != null
                        ? postgres.getServerErrorMessage().getMessage()
                        : exception.getMessage();
        return RUN_SCHEMA.matcher(String.valueOf(message)).replaceAll("");
    }

    private static Failed failed(String message, long startedAt) {
        return new Failed(new SanitizedMessage(message), elapsedMs(startedAt));
    }

    private static long elapsedMs(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }

    private static int timeoutSeconds(Duration timeout) {
        return (int) Math.max(1, (timeout.toMillis() + 999) / 1000);
    }

    private static String randomHex() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
