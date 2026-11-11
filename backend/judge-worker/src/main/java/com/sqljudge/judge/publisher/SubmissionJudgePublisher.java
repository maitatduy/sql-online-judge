package com.sqljudge.judge.publisher;

import com.sqljudge.judge.config.JudgeTopics;
import com.sqljudge.judge.job.JudgeProgress;
import com.sqljudge.judge.job.SubmissionJudgement;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SubmissionJudgePublisher {

    private final EventSender sender;
    private final Clock clock;

    SubmissionJudgePublisher(EventSender sender, Clock clock) {
        this.sender = sender;
        this.clock = clock;
    }

    public void progress(JudgeProgress progress) {
        sender.send(
                JudgeTopics.JUDGE_PROGRESS,
                progress.submissionId().toString(),
                JudgeProgressEvent.of(progress, UUID.randomUUID(), clock.instant()));
    }

    public void completed(SubmissionJudgement judgement) {
        sender.send(
                JudgeTopics.JUDGE_COMPLETED,
                judgement.submissionId().toString(),
                JudgeCompletedEvent.of(judgement, UUID.randomUUID(), clock.instant()));
    }
}
