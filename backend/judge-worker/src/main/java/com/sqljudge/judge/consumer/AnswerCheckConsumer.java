package com.sqljudge.judge.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.judge.config.JudgeTopics;
import com.sqljudge.judge.job.AnswerCheckProcessor;
import com.sqljudge.judge.publisher.AnswerCheckPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AnswerCheckConsumer {

    private static final Logger log = LoggerFactory.getLogger(AnswerCheckConsumer.class);

    private final ObjectMapper objectMapper;
    private final AnswerCheckProcessor processor;
    private final AnswerCheckPublisher publisher;

    public AnswerCheckConsumer(
            ObjectMapper objectMapper,
            AnswerCheckProcessor processor,
            AnswerCheckPublisher publisher) {
        this.objectMapper = objectMapper;
        this.processor = processor;
        this.publisher = publisher;
    }

    @KafkaListener(
            topics = JudgeTopics.ANSWER_CHECK_REQUESTED,
            concurrency = "${sqljudge.judge.consumer-concurrency}")
    public void onAnswerCheckRequested(String payload) throws JsonProcessingException {
        AnswerCheckRequestedEvent event =
                objectMapper.readValue(payload, AnswerCheckRequestedEvent.class);
        processor.process(event.toRequest(), publisher::publish);
        log.info(
                "answer_check_processed checkId={} testcases={} dialects={}",
                event.checkId(),
                event.testcases().size(),
                event.solutions().keySet());
    }
}
