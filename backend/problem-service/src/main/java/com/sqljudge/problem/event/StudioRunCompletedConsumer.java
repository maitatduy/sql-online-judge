package com.sqljudge.problem.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.problem.service.StudioRunService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StudioRunCompletedConsumer {

    private final ObjectMapper objectMapper;
    private final StudioRunService studioRunService;

    @KafkaListener(topics = StudioTopics.RUN_COMPLETED)
    public void onRunCompleted(String payload) throws JsonProcessingException {
        studioRunService.complete(objectMapper.readValue(payload, StudioRunCompletedEvent.class));
    }
}
