package com.sqljudge.submission.client;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.common.exception.ServiceUnavailableException;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class ProblemServiceFallbackFactory implements FallbackFactory<ProblemServiceClient> {

    @Override
    public ProblemServiceClient create(Throwable cause) {
        return problemId -> {
            if (cause instanceof FeignException.NotFound) {
                throw new NotFoundException(ErrorCode.PROBLEM_NOT_FOUND, "Không tìm thấy bài tập");
            }
            log.warn("problem_service_call_failed problemId={}", problemId, cause);
            throw new ServiceUnavailableException(
                    ErrorCode.PROBLEM_SERVICE_UNAVAILABLE,
                    "Chưa nhận được bài nộp, vui lòng thử lại sau");
        };
    }
}
