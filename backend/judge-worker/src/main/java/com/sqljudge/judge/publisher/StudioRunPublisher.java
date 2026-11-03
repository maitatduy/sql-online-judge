package com.sqljudge.judge.publisher;

import com.sqljudge.judge.config.JudgeTopics;
import com.sqljudge.judge.job.StudioRunResult;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class StudioRunPublisher {

    private final EventSender sender;
    private final Clock clock;

    StudioRunPublisher(EventSender sender, Clock clock) {
        this.sender = sender;
        this.clock = clock;
    }

    public void publish(StudioRunResult result) {
        StudioRunCompletedEvent event =
                StudioRunCompletedEvent.of(result, UUID.randomUUID(), clock.instant());
        sender.send(JudgeTopics.STUDIO_RUN_COMPLETED, result.runId().toString(), event);
    }
}
