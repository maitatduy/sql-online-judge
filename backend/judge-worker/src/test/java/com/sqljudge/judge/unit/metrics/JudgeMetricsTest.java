package com.sqljudge.judge.unit.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sqljudge.judge.client.JudgePackageProvider;
import com.sqljudge.judge.compare.Verdict;
import com.sqljudge.judge.consumer.SubmissionCreatedConsumer;
import com.sqljudge.judge.job.SubmissionJudgeProcessor;
import com.sqljudge.judge.job.SubmissionJudgeRequest;
import com.sqljudge.judge.job.SubmissionJudgement;
import com.sqljudge.judge.metrics.JudgeMetrics;
import com.sqljudge.judge.publisher.SubmissionJudgePublisher;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JudgeMetricsTest {

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper().registerModule(new JavaTimeModule());

    private final PrometheusMeterRegistry registry =
            new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

    @Test
    @DisplayName("Mỗi bài nộp đã chấm được đếm theo dialect và verdict, kèm thời gian chấm")
    void submissionJudged_recordsVerdictAndDuration() throws Exception {
        SubmissionJudgeProcessor processor = mock(SubmissionJudgeProcessor.class);
        when(processor.judge(any(), any()))
                .thenAnswer(
                        invocation -> {
                            SubmissionJudgeRequest request = invocation.getArgument(0);
                            return judgement(request.submissionId(), Verdict.WA);
                        });

        consumerWith(processor).onSubmissionCreated(event("MYSQL"));

        String scrape = registry.scrape();
        assertThat(scrape)
                .contains("sqljudge_judge_verdict_total{dialect=\"MYSQL\",verdict=\"WA\"} 1.0");
        assertThat(scrape)
                .contains(
                        "sqljudge_judge_duration_seconds_count{dialect=\"MYSQL\",verdict=\"WA\"}"
                                + " 1");
        assertThat(scrape).contains("sqljudge_judge_duration_seconds_bucket{");
    }

    @Test
    @DisplayName("Lỗi bất ngờ khi chấm vẫn được đếm là SE")
    void unexpectedFailure_isCountedAsSystemError() throws Exception {
        SubmissionJudgeProcessor processor = mock(SubmissionJudgeProcessor.class);
        when(processor.judge(any(), any())).thenThrow(new IllegalStateException("boom"));

        consumerWith(processor).onSubmissionCreated(event("SQLITE"));

        assertThat(registry.scrape())
                .contains("sqljudge_judge_verdict_total{dialect=\"SQLITE\",verdict=\"SE\"} 1.0");
    }

    private SubmissionCreatedConsumer consumerWith(SubmissionJudgeProcessor processor) {
        return new SubmissionCreatedConsumer(
                OBJECT_MAPPER,
                mock(JudgePackageProvider.class),
                processor,
                mock(SubmissionJudgePublisher.class),
                new JudgeMetrics(registry));
    }

    private static SubmissionJudgement judgement(UUID submissionId, Verdict verdict) {
        return new SubmissionJudgement(submissionId, 1, verdict, 12, 1, false, null, null, 1, 2);
    }

    private static String event(String dialect) {
        return """
        {"eventId": "%s", "occurredAt": "2026-11-12T08:00:00Z", "version": 1,
         "submissionId": "%s", "revisionId": "%s", "dialect": "%s",
         "code": "SELECT 1", "attempt": 1}
        """
                .formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), dialect);
    }
}
