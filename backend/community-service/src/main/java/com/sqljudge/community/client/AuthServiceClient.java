package com.sqljudge.community.client;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "auth-service",
        url = "${sqljudge.auth-service-url}",
        configuration = InternalTokenClientConfig.class,
        fallbackFactory = AuthServiceFallbackFactory.class)
public interface AuthServiceClient {

    @GetMapping("/internal/users")
    List<UserName> userNames(@RequestParam("ids") Collection<UUID> userIds);

    @GetMapping("/internal/users/by-username/{username}")
    UserAccount account(@PathVariable("username") String username);
}
