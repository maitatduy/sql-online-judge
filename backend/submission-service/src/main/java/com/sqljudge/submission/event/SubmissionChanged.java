package com.sqljudge.submission.event;

import java.util.UUID;

public record SubmissionChanged(UUID submissionId) {

    public static final String CHANNEL = "sqljudge:submission:events";
}
