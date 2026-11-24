package com.sqljudge.ai.client;

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
        return userId -> {
            log.warn("contest_service_call_failed", cause);
            throw new ServiceUnavailableException(
                    ErrorCode.CONTEST_SERVICE_UNAVAILABLE,
                    "Chưa kiểm tra được contest của bạn, vui lòng thử lại sau");
        };
    }
}
