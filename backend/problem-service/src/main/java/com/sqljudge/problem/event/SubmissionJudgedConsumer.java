package com.sqljudge.problem.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.problem.service.SubmissionStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubmissionJudgedConsumer {

    private final ObjectMapper objectMapper;
    private final SubmissionStatsService submissionStatsService;

    @KafkaListener(topics = ProblemTopics.SUBMISSION_JUDGED)
    public void onSubmissionJudged(String payload) throws JsonProcessingException {
        submissionStatsService.record(objectMapper.readValue(payload, SubmissionJudgedEvent.class));
    }
}
