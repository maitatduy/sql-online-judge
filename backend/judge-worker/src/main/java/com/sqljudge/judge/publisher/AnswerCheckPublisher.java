package com.sqljudge.judge.publisher;

import com.sqljudge.judge.config.JudgeTopics;
import com.sqljudge.judge.job.TestcaseCheckResult;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AnswerCheckPublisher {

    private final EventSender sender;
    private final Clock clock;

    AnswerCheckPublisher(EventSender sender, Clock clock) {
        this.sender = sender;
        this.clock = clock;
    }

    public void publish(TestcaseCheckResult result) {
        AnswerTestcaseCheckedEvent event =
                AnswerTestcaseCheckedEvent.of(result, UUID.randomUUID(), clock.instant());
        sender.send(JudgeTopics.ANSWER_TESTCASE_CHECKED, result.checkId().toString(), event);
    }
}
