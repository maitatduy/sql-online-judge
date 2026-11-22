package com.sqljudge.contest.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.contest.service.ContestAttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubmissionJudgedConsumer {

    private final ObjectMapper objectMapper;
    private final ContestAttemptService contestAttemptService;

    @KafkaListener(topics = ContestTopics.SUBMISSION_JUDGED)
    public void onSubmissionJudged(String payload) throws JsonProcessingException {
        contestAttemptService.record(objectMapper.readValue(payload, SubmissionJudgedEvent.class));
    }
}
