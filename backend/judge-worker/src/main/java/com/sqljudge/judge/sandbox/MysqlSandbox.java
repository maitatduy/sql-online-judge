package com.sqljudge.judge.sandbox;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

public final class MysqlSandbox extends JdbcSandbox {

    private static final Set<Integer> TIMEOUT_ERROR_CODES = Set.of(1317, 3024);
    private static final int UNKNOWN_THREAD = 1094;
    private static final Pattern RUN_ACCOUNT = Pattern.compile("'sb_[0-9a-f]{28}'@'[^']*'");
    private static final long SERVER_TIMEOUT_MARGIN_MS = 500;
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int SOCKET_TIMEOUT_MARGIN_MS = 5_000;
    private static final int MAX_RUNNER_CONNECTIONS = 2;

    private final MysqlSandboxConfig config;
    private final MysqlDdl ddl = new MysqlDdl();

    public MysqlSandbox(MysqlSandboxConfig config) {
        this.config = config;
    }

    @Override
    protected Connection connectAdmin(Duration timeout) throws SQLException {
        return connect(config.serverUrl(), config.adminUser(), config.adminPassword(), timeout);
    }

    @Override
    protected void prepare(Connection admin, String runName, String password, QueryRun run)
            throws SQLException {
        try (Statement statement = admin.createStatement()) {
            statement.execute(
                    "CREATE DATABASE "
                            + ddl.quotedNamespace(runName)
                            + " CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_as_cs");
        }
        loadDataset(admin, runName, run.dataset(), ddl);
        try (Statement statement = admin.createStatement()) {
            statement.execute(
                    "CREATE USER '"
                            + runName
                            + "'@'%' IDENTIFIED BY '"
                            + password
                            + "' WITH MAX_USER_CONNECTIONS "
                            + MAX_RUNNER_CONNECTIONS);
            for (SnapshotTable table : run.dataset().tables()) {
                statement.execute(
                        "GRANT SELECT ON "
                                + ddl.qualified(runName, table.name())
                                + " TO '"
                                + runName
                                + "'@'%'");
            }
        }
    }

    @Override
    protected Connection connectRunner(String runName, String password, Duration timeout)
            throws SQLException {
        Connection runner = connect(config.serverUrl() + runName, runName, password, timeout);
        try (Statement statement = runner.createStatement()) {
            statement.execute("SET SESSION TRANSACTION READ ONLY");
            statement.execute(
                    "SET SESSION max_execution_time = "
                            + (timeout.toMillis() + SERVER_TIMEOUT_MARGIN_MS));
            return runner;
        } catch (SQLException exception) {
            runner.close();
            throw exception;
        }
    }

    @Override
    protected void cleanUp(Connection admin, String runName) throws SQLException {
        List<Long> threads = new ArrayList<>();
        try (PreparedStatement processes =
                admin.prepareStatement(
                        "SELECT id FROM information_schema.processlist WHERE user = ?")) {
            processes.setString(1, runName);
            try (ResultSet resultSet = processes.executeQuery()) {
                while (resultSet.next()) {
                    threads.add(resultSet.getLong(1));
                }
            }
        }
        try (Statement statement = admin.createStatement()) {
            for (long thread : threads) {
                kill(statement, thread);
            }
            statement.execute("DROP DATABASE IF EXISTS " + ddl.quotedNamespace(runName));
            statement.execute("DROP USER IF EXISTS '" + runName + "'@'%'");
        }
    }

    private static void kill(Statement statement, long thread) throws SQLException {
        try {
            statement.execute("KILL " + thread);
        } catch (SQLException exception) {
            if (exception.getErrorCode() != UNKNOWN_THREAD) {
                throw exception;
            }
        }
    }

    @Override
    protected boolean isTimeout(SQLException exception) {
        return exception instanceof SQLTimeoutException
                || TIMEOUT_ERROR_CODES.contains(exception.getErrorCode());
    }

    @Override
    protected String messageOf(SQLException exception) {
        return RUN_ACCOUNT.matcher(String.valueOf(exception.getMessage())).replaceAll("");
    }

    private Connection connect(String url, String user, String password, Duration timeout)
            throws SQLException {
        Properties properties = new Properties();
        properties.setProperty("user", user);
        properties.setProperty("password", password);
        properties.setProperty("connectTimeout", String.valueOf(CONNECT_TIMEOUT_MS));
        properties.setProperty(
                "socketTimeout", String.valueOf(timeout.toMillis() + SOCKET_TIMEOUT_MARGIN_MS));
        properties.setProperty("allowMultiQueries", "false");
        properties.setProperty("allowLoadLocalInfile", "false");
        properties.setProperty("allowUrlInLocalInfile", "false");
        properties.setProperty("autoDeserialize", "false");
        return DriverManager.getConnection(url, properties);
    }
}
