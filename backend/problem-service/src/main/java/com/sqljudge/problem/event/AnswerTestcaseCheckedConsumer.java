package com.sqljudge.problem.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.problem.service.AnswerCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AnswerTestcaseCheckedConsumer {

    private final ObjectMapper objectMapper;
    private final AnswerCheckService answerCheckService;

    @KafkaListener(
            topics = StudioTopics.ANSWER_TESTCASE_CHECKED,
            properties = {"max.partition.fetch.bytes=16777216", "fetch.max.bytes=16777216"})
    public void onTestcaseChecked(String payload) throws JsonProcessingException {
        answerCheckService.record(
                objectMapper.readValue(payload, AnswerTestcaseCheckedEvent.class));
    }
}
