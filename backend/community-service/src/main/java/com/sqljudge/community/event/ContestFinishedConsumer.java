package com.sqljudge.community.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.community.service.ContestUnlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContestFinishedConsumer {

    private final ObjectMapper objectMapper;
    private final ContestUnlockService contestUnlockService;

    @KafkaListener(topics = CommunityTopics.CONTEST_FINISHED)
    public void onContestFinished(String payload) throws JsonProcessingException {
        contestUnlockService.unlock(
                objectMapper.readValue(payload, ContestFinishedEvent.class).problemIds());
    }
}
