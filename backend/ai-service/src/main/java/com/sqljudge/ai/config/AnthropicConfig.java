package com.sqljudge.ai.config;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AnthropicConfig {

    @Bean(destroyMethod = "close")
    AnthropicClient anthropicClient(AiProperties properties) {
        return AnthropicOkHttpClient.builder().apiKey(properties.apiKey()).build();
    }
}
