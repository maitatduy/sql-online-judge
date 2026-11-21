package com.sqljudge.judge.consumer;

import java.util.UUID;

public class LessonRunRetryException extends RuntimeException {

    public LessonRunRetryException(UUID runId, int deliveryAttempt) {
        super("Lesson run " + runId + " hit SE on delivery " + deliveryAttempt);
    }
}
