package com.sqljudge.judge.sandbox;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.postgresql.util.PSQLException;

public final class PostgresSandbox extends JdbcSandbox {

    private static final String QUERY_CANCELED = "57014";
    private static final int CONNECT_TIMEOUT_SECONDS = 5;
    private static final int SOCKET_TIMEOUT_MARGIN_SECONDS = 5;

    private final PostgresSandboxConfig config;
    private final PostgresDdl ddl = new PostgresDdl();

    public PostgresSandbox(PostgresSandboxConfig config, Clock clock) {
        super(clock);
        this.config = config;
    }

    @Override
    protected List<String> runNames(Connection admin) throws SQLException {
        List<String> names = new ArrayList<>();
        try (Statement statement = admin.createStatement();
                ResultSet resultSet =
                        statement.executeQuery(
                                "SELECT nspname FROM pg_namespace WHERE nspname LIKE 'sb\\_%' UNION"
                                        + " SELECT rolname FROM pg_roles WHERE rolname LIKE"
                                        + " 'sb\\_%'")) {
            while (resultSet.next()) {
                names.add(resultSet.getString(1));
            }
        }
        return names;
    }

    @Override
    protected Connection connectAdmin(Duration timeout) throws SQLException {
        return connect(config.adminUser(), config.adminPassword(), timeout);
    }

    @Override
    protected void prepare(Connection admin, String runName, String password, QueryRun run)
            throws SQLException {
        try (Statement statement = admin.createStatement()) {
            statement.execute("CREATE SCHEMA " + ddl.quotedNamespace(runName));
        }
        new SnapshotLoader(ddl).load(admin, runName, run.dataset());
        try (Statement statement = admin.createStatement()) {
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
    }

    @Override
    protected Connection connectRunner(String runName, String password, Duration timeout)
            throws SQLException {
        return connect(runName, password, timeout);
    }

    @Override
    protected void cleanUp(Connection admin, String runName) throws SQLException {
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

    @Override
    public boolean isTimeout(SQLException exception) {
        return exception instanceof SQLTimeoutException
                || QUERY_CANCELED.equals(exception.getSQLState());
    }

    @Override
    public String messageOf(SQLException exception) {
        return exception instanceof PSQLException postgres
                        && postgres.getServerErrorMessage() != null
                ? postgres.getServerErrorMessage().getMessage()
                : exception.getMessage();
    }

    private Connection connect(String user, String password, Duration timeout) throws SQLException {
        Properties properties = new Properties();
        properties.setProperty("user", user);
        properties.setProperty("password", password);
        properties.setProperty("connectTimeout", String.valueOf(CONNECT_TIMEOUT_SECONDS));
        properties.setProperty(
                "socketTimeout",
                String.valueOf(
                        QueryExecution.timeoutSeconds(timeout) + SOCKET_TIMEOUT_MARGIN_SECONDS));
        properties.setProperty("ApplicationName", "sqljudge-sandbox");
        return DriverManager.getConnection(config.jdbcUrl(), properties);
    }
}
