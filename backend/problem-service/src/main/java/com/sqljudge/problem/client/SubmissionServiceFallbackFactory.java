package com.sqljudge.problem.client;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class SubmissionServiceFallbackFactory implements FallbackFactory<SubmissionServiceClient> {

    @Override
    public SubmissionServiceClient create(Throwable cause) {
        return problemId -> {
            log.warn("submission_service_call_failed problemId={}", problemId, cause);
            throw new ServiceUnavailableException(
                    ErrorCode.SUBMISSION_SERVICE_UNAVAILABLE,
                    "Chưa lấy được số liệu bài nộp, vui lòng thử lại sau");
        };
    }
}
