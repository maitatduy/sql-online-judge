package com.sqljudge.judge.sandbox;

import com.sqljudge.judge.compare.ExecutionOutcome;
import com.sqljudge.judge.compare.ExecutionOutcome.SandboxFailed;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sqlite.ProgressHandler;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConnection;
import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;
import org.sqlite.SQLiteLimits;

public final class SqliteSandbox implements Sandbox, SqlErrors {

    private static final String MAIN_SCHEMA = "main";
    private static final int PROGRESS_STEP_INSTRUCTIONS = 1_000;
    private static final int MAX_VALUE_BYTES = 10_000_000;
    private static final List<String> DATABASE_FILE_SUFFIXES =
            List.of("", "-journal", "-wal", "-shm");
    private static final Pattern SQLITE_MESSAGE =
            Pattern.compile("^\\[SQLITE_\\w+][^(]*\\((.*)\\)$", Pattern.DOTALL);

    private final SqliteSandboxConfig config;
    private final SqliteDdl ddl = new SqliteDdl();

    public SqliteSandbox(SqliteSandboxConfig config) {
        this.config = config;
    }

    @Override
    public ExecutionOutcome run(QueryRun run) {
        Path database = config.workDirectory().resolve(RunNames.newRunName() + ".db");
        ExecutionOutcome outcome = runOn(database, run);
        try {
            for (String suffix : DATABASE_FILE_SUFFIXES) {
                Files.deleteIfExists(Path.of(database + suffix));
            }
        } catch (IOException exception) {
            return new SandboxFailed("sandbox_cleanup_failed", outcome.runtimeMs());
        }
        return outcome;
    }

    @Override
    public boolean isTimeout(SQLException exception) {
        return exception instanceof SQLiteException sqlite
                && sqlite.getResultCode() == SQLiteErrorCode.SQLITE_INTERRUPT;
    }

    @Override
    public String messageOf(SQLException exception) {
        String message = String.valueOf(exception.getMessage());
        Matcher matcher = SQLITE_MESSAGE.matcher(message);
        return matcher.matches() ? matcher.group(1) : message;
    }

    private ExecutionOutcome runOn(Path database, QueryRun run) {
        try {
            try (Connection builder = DriverManager.getConnection(urlOf(database))) {
                new SnapshotLoader(ddl).load(builder, MAIN_SCHEMA, run.dataset());
            }
            return QueryExecution.execute(
                    () -> openReadOnly(database, run.limits().timeout()), run, this);
        } catch (SQLException exception) {
            return new SandboxFailed("sandbox_unavailable", 0);
        } catch (IllegalArgumentException | DateTimeParseException exception) {
            return new SandboxFailed("invalid_dataset_snapshot", 0);
        }
    }

    private Connection openReadOnly(Path database, Duration timeout) throws SQLException {
        SQLiteConfig sqliteConfig = new SQLiteConfig();
        sqliteConfig.setReadOnly(true);
        sqliteConfig.enableLoadExtension(false);
        SQLiteConnection connection =
                (SQLiteConnection) sqliteConfig.createConnection(urlOf(database));
        try {
            connection.setLimit(SQLiteLimits.SQLITE_LIMIT_ATTACHED, 0);
            connection.setLimit(SQLiteLimits.SQLITE_LIMIT_LENGTH, MAX_VALUE_BYTES);
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA query_only = 1");
                statement.execute("PRAGMA trusted_schema = 0");
            }
            long deadline = System.nanoTime() + timeout.toNanos();
            ProgressHandler.setHandler(
                    connection,
                    PROGRESS_STEP_INSTRUCTIONS,
                    new ProgressHandler() {
                        @Override
                        protected int progress() {
                            return System.nanoTime() > deadline ? 1 : 0;
                        }
                    });
            return connection;
        } catch (SQLException exception) {
            connection.close();
            throw exception;
        }
    }

    private static String urlOf(Path database) {
        return "jdbc:sqlite:" + database.toAbsolutePath();
    }
}
