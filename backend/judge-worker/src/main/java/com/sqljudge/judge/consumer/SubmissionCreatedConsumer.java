package com.sqljudge.judge.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.judge.client.JudgePackageProvider;
import com.sqljudge.judge.client.JudgePackageUnavailableException;
import com.sqljudge.judge.compare.Verdict;
import com.sqljudge.judge.config.JudgeTopics;
import com.sqljudge.judge.job.JudgePackage;
import com.sqljudge.judge.job.SubmissionJudgeProcessor;
import com.sqljudge.judge.job.SubmissionJudgeRequest;
import com.sqljudge.judge.job.SubmissionJudgement;
import com.sqljudge.judge.publisher.SubmissionJudgePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class SubmissionCreatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(SubmissionCreatedConsumer.class);

    private final ObjectMapper objectMapper;
    private final JudgePackageProvider judgePackages;
    private final SubmissionJudgeProcessor processor;
    private final SubmissionJudgePublisher publisher;

    public SubmissionCreatedConsumer(
            ObjectMapper objectMapper,
            JudgePackageProvider judgePackages,
            SubmissionJudgeProcessor processor,
            SubmissionJudgePublisher publisher) {
        this.objectMapper = objectMapper;
        this.judgePackages = judgePackages;
        this.processor = processor;
        this.publisher = publisher;
    }

    @KafkaListener(
            topics = JudgeTopics.SUBMISSION_CREATED,
            concurrency = "${sqljudge.judge.consumer-concurrency}")
    public void onSubmissionCreated(String payload) throws JsonProcessingException {
        SubmissionCreatedEvent event =
                objectMapper.readValue(payload, SubmissionCreatedEvent.class);
        SubmissionJudgement judgement = judge(event);
        publisher.completed(judgement);
        log.info(
                "submission_judged submissionId={} attempt={} verdict={} codeLength={}",
                event.submissionId(),
                event.attempt(),
                judgement.verdict(),
                event.code().length());
    }

    private SubmissionJudgement judge(SubmissionCreatedEvent event) {
        JudgePackage judgePackage;
        try {
            judgePackage = judgePackages.packageOf(event.revisionId());
        } catch (JudgePackageUnavailableException exception) {
            log.warn(
                    "judge_package_unavailable submissionId={} revisionId={}",
                    event.submissionId(),
                    event.revisionId(),
                    exception);
            return systemError(event);
        }
        try {
            return processor.judge(
                    new SubmissionJudgeRequest(
                            event.submissionId(),
                            event.attempt(),
                            event.dialect(),
                            event.code(),
                            judgePackage),
                    publisher::progress);
        } catch (RuntimeException exception) {
            log.error(
                    "submission_judge_failed submissionId={} attempt={}",
                    event.submissionId(),
                    event.attempt(),
                    exception);
            return systemError(event);
        }
    }

    private static SubmissionJudgement systemError(SubmissionCreatedEvent event) {
        return new SubmissionJudgement(
                event.submissionId(), event.attempt(), Verdict.SE, 0, null, null, null, null, 0, 0);
    }
}
