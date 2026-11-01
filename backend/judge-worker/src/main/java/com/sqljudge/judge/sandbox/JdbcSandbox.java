package com.sqljudge.judge.sandbox;

import com.sqljudge.judge.compare.ExecutionOutcome;
import com.sqljudge.judge.compare.ExecutionOutcome.SandboxFailed;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.format.DateTimeParseException;

public abstract class JdbcSandbox implements Sandbox, SqlErrors {

    @Override
    public final ExecutionOutcome run(QueryRun run) {
        String runName = RunNames.newRunName();
        try (Connection admin = connectAdmin(run.limits().timeout())) {
            try {
                String password = RunNames.randomHex();
                prepare(admin, runName, password, run);
                return QueryExecution.execute(
                        () -> connectRunner(runName, password, run.limits().timeout()), run, this);
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
}
