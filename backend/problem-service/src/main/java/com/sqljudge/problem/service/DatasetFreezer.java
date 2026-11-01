package com.sqljudge.problem.service;

import com.sqljudge.problem.entity.Dataset;
import com.sqljudge.problem.entity.DatasetVersion;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DatasetFreezer {

    private final Clock clock;

    public DatasetVersion freezeLatest(Dataset dataset) {
        DatasetVersion latest = dataset.latestVersion();
        if (latest.isDraft()) {
            DatasetReadiness.check(latest);
            latest.setChecksum(DatasetChecksum.of(latest));
            latest.setFrozenAt(clock.instant());
        }
        return latest;
    }
}
