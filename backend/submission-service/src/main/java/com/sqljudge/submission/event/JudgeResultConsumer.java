package com.sqljudge.submission.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.submission.service.JudgeResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JudgeResultConsumer {

    private final ObjectMapper objectMapper;
    private final JudgeResultService judgeResultService;

    @KafkaListener(topics = SubmissionTopics.JUDGE_PROGRESS)
    void onProgress(String payload) throws JsonProcessingException {
        JudgeProgressEvent event = objectMapper.readValue(payload, JudgeProgressEvent.class);
        judgeResultService.recordProgress(
                event.submissionId(), event.attempt(), event.judgedTestcases());
    }

    @KafkaListener(topics = SubmissionTopics.JUDGE_COMPLETED)
    void onCompleted(String payload) throws JsonProcessingException {
        JudgeCompletedEvent event = objectMapper.readValue(payload, JudgeCompletedEvent.class);
        judgeResultService.complete(event.submissionId(), event.toResult());
    }
}
