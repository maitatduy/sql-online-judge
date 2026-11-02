package com.sqljudge.judge.client;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
class ProblemServiceFallbackFactory implements FallbackFactory<ProblemServiceClient> {

    @Override
    public ProblemServiceClient create(Throwable cause) {
        return versionId -> {
            throw new SnapshotUnavailableException(
                    "Cannot load dataset snapshot " + versionId, cause);
        };
    }
}
