package com.sqljudge.problem.client;

import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "submission-service",
        url = "${sqljudge.submission-service-url}",
        configuration = InternalTokenClientConfig.class,
        fallbackFactory = SubmissionServiceFallbackFactory.class)
public interface SubmissionServiceClient {

    @GetMapping("/internal/problems/{problemId}/judging-summary")
    JudgingSummary judgingSummary(@PathVariable("problemId") UUID problemId);
}
