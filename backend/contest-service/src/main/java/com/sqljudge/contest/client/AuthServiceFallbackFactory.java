package com.sqljudge.contest.client;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.common.exception.ServiceUnavailableException;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class AuthServiceFallbackFactory implements FallbackFactory<AuthServiceClient> {

    @Override
    public AuthServiceClient create(Throwable cause) {
        return userId -> {
            if (cause instanceof FeignException.NotFound) {
                throw new NotFoundException(ErrorCode.USER_NOT_FOUND, "Không tìm thấy người dùng");
            }
            log.warn("auth_service_call_failed userId={}", userId, cause);
            throw new ServiceUnavailableException(
                    ErrorCode.AUTH_SERVICE_UNAVAILABLE,
                    "Chưa kiểm tra được tài khoản, vui lòng thử lại sau");
        };
    }
}
