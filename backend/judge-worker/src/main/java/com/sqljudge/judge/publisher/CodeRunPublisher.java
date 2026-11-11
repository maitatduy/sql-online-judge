package com.sqljudge.judge.publisher;

import com.sqljudge.judge.config.JudgeTopics;
import com.sqljudge.judge.job.CodeRunResult;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CodeRunPublisher {

    private final EventSender sender;
    private final Clock clock;

    CodeRunPublisher(EventSender sender, Clock clock) {
        this.sender = sender;
        this.clock = clock;
    }

    public void completed(CodeRunResult result) {
        sender.send(
                JudgeTopics.RUN_COMPLETED,
                result.runId().toString(),
                RunCompletedEvent.of(result, UUID.randomUUID(), clock.instant()));
    }
}
