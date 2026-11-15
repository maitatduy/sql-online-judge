package com.sqljudge.learning.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.learning.service.LessonRunService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LessonRunCompletedConsumer {

    private final ObjectMapper objectMapper;
    private final LessonRunService lessonRunService;

    @KafkaListener(topics = LearningTopics.LESSON_RUN_COMPLETED)
    public void onLessonRunCompleted(String payload) throws JsonProcessingException {
        LessonRunCompletedEvent event =
                objectMapper.readValue(payload, LessonRunCompletedEvent.class);
        lessonRunService.complete(event.runId(), event.toCompletion());
    }
}
