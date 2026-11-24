package com.sqljudge.notification.client;

import java.util.List;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "auth-service",
        url = "${sqljudge.auth-service-url}",
        configuration = InternalTokenClientConfig.class)
public interface AuthServiceClient {

    @GetMapping("/internal/users/{userId}/contact")
    UserContact contact(@PathVariable("userId") UUID userId);

    @GetMapping("/internal/users/active-ids")
    List<UUID> activeIds(
            @RequestParam(value = "afterId", required = false) UUID afterId,
            @RequestParam("size") int size);
}
