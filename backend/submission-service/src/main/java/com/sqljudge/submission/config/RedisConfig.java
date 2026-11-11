package com.sqljudge.submission.config;

import com.sqljudge.submission.event.SubmissionChanged;
import com.sqljudge.submission.service.SubmissionStreamService;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class RedisConfig {

    @Bean
    RedisMessageListenerContainer submissionChangeListener(
            RedisConnectionFactory connectionFactory, SubmissionStreamService streamService) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(
                (message, pattern) ->
                        streamService.onChanged(
                                UUID.fromString(
                                        new String(message.getBody(), StandardCharsets.UTF_8))),
                new ChannelTopic(SubmissionChanged.CHANNEL));
        return container;
    }
}
