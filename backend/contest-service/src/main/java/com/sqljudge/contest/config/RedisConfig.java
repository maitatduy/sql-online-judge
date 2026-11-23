package com.sqljudge.contest.config;

import com.sqljudge.contest.service.StandingsStreamService;
import com.sqljudge.contest.standings.StandingsStore;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class RedisConfig {

    @Bean
    @ConditionalOnProperty(
            name = "sqljudge.contest.standings-listener-enabled",
            havingValue = "true",
            matchIfMissing = true)
    RedisMessageListenerContainer standingsChangeListener(
            RedisConnectionFactory connectionFactory, StandingsStreamService streamService) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(
                (message, pattern) ->
                        streamService.markChanged(
                                UUID.fromString(
                                        new String(message.getBody(), StandardCharsets.UTF_8))),
                new ChannelTopic(StandingsStore.CHANGED_CHANNEL));
        return container;
    }
}
