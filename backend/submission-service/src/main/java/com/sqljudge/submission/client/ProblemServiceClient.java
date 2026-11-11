package com.sqljudge.submission.client;

import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "problem-service",
        url = "${sqljudge.problem-service-url}",
        configuration = ProblemServiceClientConfig.class,
        fallbackFactory = ProblemServiceFallbackFactory.class)
public interface ProblemServiceClient {

    @GetMapping("/internal/problems/{problemId}/judging-info")
    JudgingInfo judgingInfo(@PathVariable("problemId") UUID problemId);
}
