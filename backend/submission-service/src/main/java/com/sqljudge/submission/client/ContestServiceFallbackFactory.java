package com.sqljudge.submission.client;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class ContestServiceFallbackFactory implements FallbackFactory<ContestServiceClient> {

    @Override
    public ContestServiceClient create(Throwable cause) {
        return (slug, letter, userId) -> {
            RemoteErrors.ruleViolationOf(cause)
                    .ifPresent(
                            violation -> {
                                throw violation;
                            });
            log.warn("contest_service_call_failed slug={} letter={}", slug, letter, cause);
            throw new ServiceUnavailableException(
                    ErrorCode.CONTEST_SERVICE_UNAVAILABLE,
                    "Chưa kiểm tra được quyền nộp bài, vui lòng thử lại sau");
        };
    }
}
