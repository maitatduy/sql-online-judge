package com.sqljudge.judge.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.judge.client.JudgePackageProvider;
import com.sqljudge.judge.client.JudgePackageUnavailableException;
import com.sqljudge.judge.config.JudgeTopics;
import com.sqljudge.judge.job.CodeRunProcessor;
import com.sqljudge.judge.job.CodeRunRequest;
import com.sqljudge.judge.job.CodeRunResult;
import com.sqljudge.judge.job.RunOutcome;
import com.sqljudge.judge.publisher.CodeRunPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class RunRequestedConsumer {

    private static final Logger log = LoggerFactory.getLogger(RunRequestedConsumer.class);

    private final ObjectMapper objectMapper;
    private final JudgePackageProvider judgePackages;
    private final CodeRunProcessor processor;
    private final CodeRunPublisher publisher;

    public RunRequestedConsumer(
            ObjectMapper objectMapper,
            JudgePackageProvider judgePackages,
            CodeRunProcessor processor,
            CodeRunPublisher publisher) {
        this.objectMapper = objectMapper;
        this.judgePackages = judgePackages;
        this.processor = processor;
        this.publisher = publisher;
    }

    @KafkaListener(
            topics = JudgeTopics.RUN_REQUESTED,
            concurrency = "${sqljudge.judge.consumer-concurrency}")
    public void onRunRequested(String payload) throws JsonProcessingException {
        RunRequestedEvent event = objectMapper.readValue(payload, RunRequestedEvent.class);
        CodeRunResult result = run(event);
        publisher.completed(result);
        log.info(
                "code_run_completed runId={} outcome={} codeLength={}",
                event.runId(),
                result.outcome(),
                event.code().length());
    }

    private CodeRunResult run(RunRequestedEvent event) {
        try {
            return processor.run(
                    new CodeRunRequest(
                            event.runId(),
                            event.dialect(),
                            event.code(),
                            judgePackages.packageOf(event.revisionId())));
        } catch (JudgePackageUnavailableException exception) {
            log.warn(
                    "judge_package_unavailable runId={} revisionId={}",
                    event.runId(),
                    event.revisionId(),
                    exception);
            return CodeRunResult.failed(event.runId(), RunOutcome.SE, 0, null);
        } catch (RuntimeException exception) {
            log.error("code_run_failed runId={}", event.runId(), exception);
            return CodeRunResult.failed(event.runId(), RunOutcome.SE, 0, null);
        }
    }
}
