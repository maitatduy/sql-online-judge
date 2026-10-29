package com.sqljudge.problem.config;

import com.sqljudge.problem.entity.Dialect;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("sqljudge.judge")
public record JudgeEngineProperties(Map<Dialect, String> engineVersions) {

    public JudgeEngineProperties {
        engineVersions = Map.copyOf(engineVersions);
    }
}
