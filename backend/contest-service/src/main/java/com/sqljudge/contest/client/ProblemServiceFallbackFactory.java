package com.sqljudge.contest.client;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class ProblemServiceFallbackFactory implements FallbackFactory<ProblemServiceClient> {

    @Override
    public ProblemServiceClient create(Throwable cause) {
        return problemIds -> {
            log.warn("problem_service_call_failed problemCount={}", problemIds.size(), cause);
            throw new ServiceUnavailableException(
                    ErrorCode.PROBLEM_SERVICE_UNAVAILABLE,
                    "Chưa kiểm tra được bài tập, vui lòng thử lại sau");
        };
    }
}
