package com.sqljudge.submission.client;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

class ProblemServiceClientConfig {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    @Bean
    RequestInterceptor internalTokenInterceptor(
            @Value("${sqljudge.internal.api-token}") String internalToken) {
        return template -> template.header(INTERNAL_TOKEN_HEADER, internalToken);
    }
}
