package com.sqljudge.gateway.ratelimit;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpMethod;

@ConfigurationProperties("sqljudge.rate-limit")
public record RateLimitProperties(int trustedProxies, List<Policy> policies) {

    public RateLimitProperties {
        if (trustedProxies < 0) {
            throw new IllegalArgumentException("trusted-proxies must not be negative");
        }
        policies = policies == null ? List.of() : List.copyOf(policies);
    }

    public record Policy(
            String name, String path, String method, ClientKey key, int limit, Duration window) {

        public Policy {
            method = method == null ? HttpMethod.POST.name() : method;
            key = key == null ? ClientKey.IP : key;
        }
    }

    public enum ClientKey {
        IP,
        USER
    }
}
