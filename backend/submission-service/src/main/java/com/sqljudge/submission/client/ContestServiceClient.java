package com.sqljudge.submission.client;

import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "contest-service",
        url = "${sqljudge.contest-service-url}",
        configuration = InternalTokenClientConfig.class,
        fallbackFactory = ContestServiceFallbackFactory.class)
public interface ContestServiceClient {

    @GetMapping("/internal/contests/{slug}/problems/{letter}/entry")
    ContestEntry entry(
            @PathVariable("slug") String slug,
            @PathVariable("letter") String letter,
            @RequestParam("userId") UUID userId);
}
