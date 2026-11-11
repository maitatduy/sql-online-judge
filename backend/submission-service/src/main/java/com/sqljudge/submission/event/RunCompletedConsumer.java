package com.sqljudge.submission.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.submission.service.CodeRunService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RunCompletedConsumer {

    private final ObjectMapper objectMapper;
    private final CodeRunService codeRunService;

    @KafkaListener(topics = SubmissionTopics.RUN_COMPLETED)
    void onRunCompleted(String payload) throws JsonProcessingException {
        RunCompletedEvent event = objectMapper.readValue(payload, RunCompletedEvent.class);
        codeRunService.complete(event.runId(), event.toCompletion());
    }
}
