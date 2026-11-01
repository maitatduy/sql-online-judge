package com.sqljudge.problem.service;

import com.sqljudge.problem.dto.response.StudioDatasetResponse;
import com.sqljudge.problem.entity.Dataset;
import com.sqljudge.problem.entity.DatasetVersion;
import com.sqljudge.problem.repository.DatasetRepository;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudioDatasetVersionService {

    private final DatasetRepository datasetRepository;
    private final DatasetAccess datasetAccess;
    private final Clock clock;

    @Transactional
    public StudioDatasetResponse freeze(UUID datasetId) {
        Dataset dataset = datasetAccess.editable(datasetId);
        DatasetVersion latest = dataset.latestVersion();
        if (!latest.isDraft()) {
            return StudioDatasetResponse.from(dataset);
        }
        DatasetReadiness.check(latest);
        latest.setChecksum(DatasetChecksum.of(latest));
        latest.setFrozenAt(clock.instant());
        return StudioDatasetResponse.from(datasetRepository.saveAndFlush(dataset));
    }
}
