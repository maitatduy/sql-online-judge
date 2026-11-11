package com.sqljudge.judge.client;

import java.util.UUID;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
class ProblemServiceFallbackFactory implements FallbackFactory<ProblemServiceClient> {

    @Override
    public ProblemServiceClient create(Throwable cause) {
        return new ProblemServiceClient() {
            @Override
            public DatasetSnapshotResponse snapshot(UUID versionId) {
                throw new SnapshotUnavailableException(
                        "Cannot load dataset snapshot " + versionId, cause);
            }

            @Override
            public JudgePackageResponse judgePackage(UUID revisionId) {
                throw new JudgePackageUnavailableException(
                        "Cannot load judge package " + revisionId, cause);
            }
        };
    }
}
