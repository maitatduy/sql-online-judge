package com.sqljudge.problem.client;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class AuthServiceFallbackFactory implements FallbackFactory<AuthServiceClient> {

    @Override
    public AuthServiceClient create(Throwable cause) {
        return userIds -> {
            log.warn("auth_service_call_failed userCount={}", userIds.size(), cause);
            return List.of();
        };
    }
}
