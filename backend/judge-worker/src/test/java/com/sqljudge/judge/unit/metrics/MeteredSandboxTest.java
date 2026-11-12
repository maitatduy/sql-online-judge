package com.sqljudge.judge.unit.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.judge.compare.ExecutionOutcome;
import com.sqljudge.judge.compare.ExecutionOutcome.SandboxFailed;
import com.sqljudge.judge.compare.ExecutionOutcome.TimedOut;
import com.sqljudge.judge.metrics.JudgeMetrics;
import com.sqljudge.judge.metrics.MeteredSandbox;
import com.sqljudge.judge.sandbox.Dialect;
import com.sqljudge.judge.sandbox.Sandbox;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MeteredSandboxTest {

    private final PrometheusMeterRegistry registry =
            new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

    @Test
    @DisplayName("Sandbox lỗi được đếm theo dialect và lý do, kết quả trả về giữ nguyên")
    void run_sandboxFailure_isCountedByReason() {
        ExecutionOutcome failure = new SandboxFailed("sandbox_unavailable", 0);
        Sandbox sandbox = meteredSandbox(failure);

        ExecutionOutcome outcome = sandbox.run(null);

        assertThat(outcome).isSameAs(failure);
        assertThat(registry.scrape())
                .contains(
                        "sqljudge_judge_sandbox_errors_total{dialect=\"POSTGRESQL\","
                                + "reason=\"sandbox_unavailable\"} 1.0");
    }

    @Test
    @DisplayName("Kết quả không phải lỗi sandbox thì không đếm")
    void run_otherOutcome_isNotCounted() {
        meteredSandbox(new TimedOut(2000)).run(null);

        assertThat(registry.scrape()).doesNotContain("sqljudge_judge_sandbox_errors_total{");
    }

    private Sandbox meteredSandbox(ExecutionOutcome outcome) {
        return new MeteredSandbox(Dialect.POSTGRESQL, run -> outcome, new JudgeMetrics(registry));
    }
}
