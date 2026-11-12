package com.sqljudge.submission.dto.response;

import com.sqljudge.submission.entity.Dialect;
import com.sqljudge.submission.entity.Submission;
import java.util.UUID;

public record SubmissionCodeResponse(UUID id, Dialect dialect, String code) {

    public static SubmissionCodeResponse from(Submission submission) {
        return new SubmissionCodeResponse(
                submission.getId(), submission.getDialect(), submission.getCode());
    }
}
