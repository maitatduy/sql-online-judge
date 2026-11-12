package com.sqljudge.judge.sandbox;

import com.sqljudge.judge.compare.ExecutionOutcome;
import com.sqljudge.judge.compare.ExecutionOutcome.SandboxFailed;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

public abstract class JdbcSandbox implements Sandbox, SqlErrors, OrphanCleaner {

    private static final Duration ADMIN_TIMEOUT = Duration.ofSeconds(10);

    private final Clock clock;

    protected JdbcSandbox(Clock clock) {
        this.clock = clock;
    }

    @Override
    public final ExecutionOutcome run(QueryRun run) {
        String runName = RunNames.newRunName(clock.instant());
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

    @Override
    public final int removeRunsCreatedBefore(Instant cutoff) {
        try (Connection admin = connectAdmin(ADMIN_TIMEOUT)) {
            List<String> staleRuns =
                    runNames(admin).stream()
                            .filter(name -> RunNames.wasCreatedBefore(name, cutoff))
                            .toList();
            for (String runName : staleRuns) {
                cleanUp(admin, runName);
            }
            return staleRuns.size();
        } catch (SQLException exception) {
            throw new OrphanCleanupException("Could not remove stale sandbox runs", exception);
        }
    }

    protected abstract Connection connectAdmin(Duration timeout) throws SQLException;

    protected abstract void prepare(Connection admin, String runName, String password, QueryRun run)
            throws SQLException;

    protected abstract Connection connectRunner(String runName, String password, Duration timeout)
            throws SQLException;

    protected abstract void cleanUp(Connection admin, String runName) throws SQLException;

    protected abstract List<String> runNames(Connection admin) throws SQLException;
}
