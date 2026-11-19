package com.sqljudge.learning.client;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.BusinessRuleException;
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
        return variantId -> {
            if (cause instanceof FeignException.NotFound) {
                throw new BusinessRuleException(
                        ErrorCode.DATASET_VARIANT_NOT_FOUND, "Không tìm thấy bộ dữ liệu");
            }
            log.warn("problem_service_call_failed variantId={}", variantId, cause);
            throw new ServiceUnavailableException(
                    ErrorCode.PROBLEM_SERVICE_UNAVAILABLE,
                    "Chưa kiểm tra được bộ dữ liệu, vui lòng thử lại sau");
        };
    }
}
