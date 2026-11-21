package com.sqljudge.problem.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.problem.service.ContestProblemOpeningService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContestFinishedConsumer {

    private final ObjectMapper objectMapper;
    private final ContestProblemOpeningService contestProblemOpeningService;

    @KafkaListener(topics = ProblemTopics.CONTEST_FINISHED)
    public void onContestFinished(String payload) throws JsonProcessingException {
        contestProblemOpeningService.open(
                objectMapper.readValue(payload, ContestFinishedEvent.class));
    }
}
