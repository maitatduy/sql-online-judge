package com.sqljudge.contest.client;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.common.exception.ServiceUnavailableException;
import feign.FeignException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class AuthServiceFallbackFactory implements FallbackFactory<AuthServiceClient> {

    @Override
    public AuthServiceClient create(Throwable cause) {
        return new AuthServiceClient() {
            @Override
            public ContestantInfo user(UUID userId) {
                if (cause instanceof FeignException.NotFound) {
                    throw new NotFoundException(
                            ErrorCode.USER_NOT_FOUND, "Không tìm thấy người dùng");
                }
                log.warn("auth_service_call_failed userId={}", userId, cause);
                throw new ServiceUnavailableException(
                        ErrorCode.AUTH_SERVICE_UNAVAILABLE,
                        "Chưa kiểm tra được tài khoản, vui lòng thử lại sau");
            }

            @Override
            public List<UserName> userNames(Collection<UUID> userIds) {
                log.warn("auth_service_call_failed userCount={}", userIds.size(), cause);
                return List.of();
            }
        };
    }
}
