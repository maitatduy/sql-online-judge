package com.sqljudge.ai.client;

import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "contest-service",
        url = "${sqljudge.contest-service-url}",
        configuration = InternalTokenClientConfig.class,
        fallbackFactory = ContestServiceFallbackFactory.class)
public interface ContestServiceClient {

    @GetMapping("/internal/contests/rated-participation/{userId}")
    RatedParticipation ratedParticipation(@PathVariable("userId") UUID userId);
}
