package com.sqljudge.contest.client;

import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "auth-service",
        url = "${sqljudge.auth-service-url}",
        configuration = InternalTokenClientConfig.class,
        fallbackFactory = AuthServiceFallbackFactory.class)
public interface AuthServiceClient {

    @GetMapping("/internal/users/{userId}")
    ContestantInfo user(@PathVariable("userId") UUID userId);
}
