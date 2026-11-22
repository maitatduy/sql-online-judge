package com.sqljudge.contest.client;

import com.sqljudge.contest.entity.ContestProblemInfo;
import java.util.List;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "problem-service",
        url = "${sqljudge.problem-service-url}",
        configuration = InternalTokenClientConfig.class,
        fallbackFactory = ProblemServiceFallbackFactory.class)
public interface ProblemServiceClient {

    @GetMapping("/internal/problems/contest-ready")
    List<ContestProblemInfo> contestReady(@RequestParam("ids") List<UUID> problemIds);

    @GetMapping("/internal/problems/{problemId}/contest-statement")
    ContestStatement contestStatement(@PathVariable("problemId") UUID problemId);
}
