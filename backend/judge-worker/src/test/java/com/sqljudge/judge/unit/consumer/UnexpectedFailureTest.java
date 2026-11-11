package com.sqljudge.judge.unit.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sqljudge.judge.client.JudgePackageProvider;
import com.sqljudge.judge.compare.Verdict;
import com.sqljudge.judge.consumer.RunRequestedConsumer;
import com.sqljudge.judge.consumer.SubmissionCreatedConsumer;
import com.sqljudge.judge.job.CodeRunProcessor;
import com.sqljudge.judge.job.CodeRunResult;
import com.sqljudge.judge.job.RunOutcome;
import com.sqljudge.judge.job.SubmissionJudgeProcessor;
import com.sqljudge.judge.job.SubmissionJudgement;
import com.sqljudge.judge.publisher.CodeRunPublisher;
import com.sqljudge.judge.publisher.SubmissionJudgePublisher;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UnexpectedFailureTest {

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    @DisplayName(
            "Lỗi bất ngờ khi chấm bài nộp thì gửi SE để chấm lại, không bỏ bài nộp kẹt PENDING")
    void submissionCreated_unexpectedFailure_publishesSystemError() throws Exception {
        UUID submissionId = UUID.randomUUID();
        SubmissionJudgeProcessor processor = mock(SubmissionJudgeProcessor.class);
        when(processor.judge(any(), any())).thenThrow(new IllegalStateException("boom"));
        SubmissionJudgePublisher publisher = mock(SubmissionJudgePublisher.class);
        SubmissionCreatedConsumer consumer =
                new SubmissionCreatedConsumer(
                        OBJECT_MAPPER, mock(JudgePackageProvider.class), processor, publisher);

        consumer.onSubmissionCreated(
                """
                {"eventId": "%s", "occurredAt": "2026-11-12T08:00:00Z", "version": 1,
                 "submissionId": "%s", "revisionId": "%s", "dialect": "SQLITE",
                 "code": "SELECT 1", "attempt": 2}
                """
                        .formatted(UUID.randomUUID(), submissionId, UUID.randomUUID()));

        ArgumentCaptor<SubmissionJudgement> judgement =
                ArgumentCaptor.forClass(SubmissionJudgement.class);
        verify(publisher).completed(judgement.capture());
        assertThat(judgement.getValue().submissionId()).isEqualTo(submissionId);
        assertThat(judgement.getValue().attempt()).isEqualTo(2);
        assertThat(judgement.getValue().verdict()).isEqualTo(Verdict.SE);
        assertThat(judgement.getValue().message()).isNull();
    }

    @Test
    @DisplayName("Lỗi bất ngờ khi chạy thử thì gửi SE thay vì bỏ lần chạy treo")
    void runRequested_unexpectedFailure_publishesSystemError() throws Exception {
        UUID runId = UUID.randomUUID();
        CodeRunProcessor processor = mock(CodeRunProcessor.class);
        when(processor.run(any())).thenThrow(new IllegalStateException("boom"));
        CodeRunPublisher publisher = mock(CodeRunPublisher.class);
        RunRequestedConsumer consumer =
                new RunRequestedConsumer(
                        OBJECT_MAPPER, mock(JudgePackageProvider.class), processor, publisher);

        consumer.onRunRequested(
                """
                {"eventId": "%s", "occurredAt": "2026-11-12T08:00:00Z", "version": 1,
                 "runId": "%s", "revisionId": "%s", "dialect": "SQLITE", "code": "SELECT 1"}
                """
                        .formatted(UUID.randomUUID(), runId, UUID.randomUUID()));

        ArgumentCaptor<CodeRunResult> result = ArgumentCaptor.forClass(CodeRunResult.class);
        verify(publisher).completed(result.capture());
        assertThat(result.getValue().runId()).isEqualTo(runId);
        assertThat(result.getValue().outcome()).isEqualTo(RunOutcome.SE);
        assertThat(result.getValue().message()).isNull();
    }
}
