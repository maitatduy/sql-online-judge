package com.sqljudge.problem.client;

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
        return problemId -> {
            log.warn("contest_service_call_failed problemId={}", problemId, cause);
            throw new ServiceUnavailableException(
                    ErrorCode.CONTEST_SERVICE_UNAVAILABLE,
                    "Chưa kiểm tra được contest của bài, vui lòng thử lại sau");
        };
    }
}
