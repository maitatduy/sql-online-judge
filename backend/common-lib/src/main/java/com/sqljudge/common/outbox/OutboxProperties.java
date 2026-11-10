package com.sqljudge.common.outbox;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("sqljudge.outbox")
public record OutboxProperties(
        Duration relayInterval, int batchSize, Duration sendTimeout, Duration claimTimeout) {}
