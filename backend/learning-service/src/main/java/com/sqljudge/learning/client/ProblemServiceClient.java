package com.sqljudge.learning.client;

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

    @GetMapping("/internal/dataset-variants/{variantId}")
    DatasetVariantInfo datasetVariant(@PathVariable("variantId") UUID variantId);
}
