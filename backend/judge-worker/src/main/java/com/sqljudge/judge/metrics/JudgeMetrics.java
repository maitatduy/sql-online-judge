package com.sqljudge.judge.metrics;

import com.sqljudge.judge.compare.Verdict;
import com.sqljudge.judge.sandbox.Dialect;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class JudgeMetrics {

    private static final Duration MIN_EXPECTED_DURATION = Duration.ofMillis(10);
    private static final Duration MAX_EXPECTED_DURATION = Duration.ofMinutes(2);

    private final MeterRegistry registry;

    public JudgeMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public Timer.Sample startJudging() {
        return Timer.start(registry);
    }

    public void judged(Timer.Sample sample, Dialect dialect, Verdict verdict) {
        sample.stop(
                Timer.builder("sqljudge.judge.duration")
                        .description("Thời gian chấm một bài nộp")
                        .tag("dialect", dialect.name())
                        .tag("verdict", verdict.name())
                        .publishPercentileHistogram()
                        .minimumExpectedValue(MIN_EXPECTED_DURATION)
                        .maximumExpectedValue(MAX_EXPECTED_DURATION)
                        .register(registry));
        Counter.builder("sqljudge.judge.verdict")
                .description("Số bài nộp đã chấm theo verdict")
                .tag("dialect", dialect.name())
                .tag("verdict", verdict.name())
                .register(registry)
                .increment();
    }

    public void sandboxFailed(Dialect dialect, String reason) {
        Counter.builder("sqljudge.judge.sandbox.errors")
                .description("Số lần sandbox không chạy được")
                .tag("dialect", dialect.name())
                .tag("reason", reason)
                .register(registry)
                .increment();
    }
}
