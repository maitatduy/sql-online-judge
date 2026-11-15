package com.sqljudge.judge.unit.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sqljudge.judge.consumer.LessonRunConsumer;
import com.sqljudge.judge.job.CodeRunResult;
import com.sqljudge.judge.job.LessonRunProcessor;
import com.sqljudge.judge.job.LessonRunRequest;
import com.sqljudge.judge.job.RunOutcome;
import com.sqljudge.judge.publisher.LessonRunPublisher;
import com.sqljudge.judge.sandbox.Dialect;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class LessonRunConsumerTest {

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper().registerModule(new JavaTimeModule());

    private final UUID runId = UUID.randomUUID();
    private final UUID versionId = UUID.randomUUID();
    private final UUID variantId = UUID.randomUUID();
    private final LessonRunProcessor processor = mock(LessonRunProcessor.class);
    private final LessonRunPublisher publisher = mock(LessonRunPublisher.class);
    private final LessonRunConsumer consumer =
            new LessonRunConsumer(OBJECT_MAPPER, processor, publisher);

    @Test
    @DisplayName("Yêu cầu chạy ô bài giảng được chạy đúng dataset, variant và giới hạn giờ")
    void lessonRunRequested_runsAndPublishesResult() throws Exception {
        CodeRunResult expected =
                new CodeRunResult(
                        runId, RunOutcome.OK, List.of("id"), List.of(List.of("1")), false, 3, null);
        when(processor.run(any())).thenReturn(expected);

        consumer.onLessonRunRequested(event());

        ArgumentCaptor<LessonRunRequest> request = ArgumentCaptor.forClass(LessonRunRequest.class);
        verify(processor).run(request.capture());
        assertThat(request.getValue())
                .isEqualTo(
                        new LessonRunRequest(
                                runId,
                                versionId,
                                variantId,
                                "abc123",
                                Dialect.POSTGRESQL,
                                "SELECT 1",
                                2000));
        verify(publisher).completed(expected);
    }

    @Test
    @DisplayName("Lỗi bất ngờ khi chạy ô bài giảng thì gửi SE thay vì bỏ lần chạy treo")
    void lessonRunRequested_unexpectedFailure_publishesSystemError() throws Exception {
        when(processor.run(any())).thenThrow(new IllegalStateException("boom"));

        consumer.onLessonRunRequested(event());

        ArgumentCaptor<CodeRunResult> result = ArgumentCaptor.forClass(CodeRunResult.class);
        verify(publisher).completed(result.capture());
        assertThat(result.getValue().runId()).isEqualTo(runId);
        assertThat(result.getValue().outcome()).isEqualTo(RunOutcome.SE);
        assertThat(result.getValue().message()).isNull();
    }

    private String event() {
        return """
        {"eventId": "%s", "occurredAt": "2026-11-15T08:00:00Z", "version": 1,
         "runId": "%s", "datasetVersionId": "%s", "datasetVariantId": "%s",
         "datasetChecksum": "abc123", "dialect": "POSTGRESQL", "sql": "SELECT 1",
         "timeLimitMs": 2000}
        """
                .formatted(UUID.randomUUID(), runId, versionId, variantId);
    }
}
