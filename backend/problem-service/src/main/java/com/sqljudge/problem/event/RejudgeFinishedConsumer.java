package com.sqljudge.problem.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.problem.client.SubmissionServiceClient;
import com.sqljudge.problem.service.ProblemStatsRecomputeService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RejudgeFinishedConsumer {

    private final ObjectMapper objectMapper;
    private final SubmissionServiceClient submissionServiceClient;
    private final ProblemStatsRecomputeService recomputeService;

    @KafkaListener(topics = ProblemTopics.REJUDGE_FINISHED)
    public void onRejudgeFinished(String payload) throws JsonProcessingException {
        RejudgeFinishedEvent event = objectMapper.readValue(payload, RejudgeFinishedEvent.class);
        if (recomputeService.isProcessed(event.eventId())) {
            return;
        }
        recomputeService.replace(event, submissionServiceClient.judgingSummary(event.problemId()));
    }
}
