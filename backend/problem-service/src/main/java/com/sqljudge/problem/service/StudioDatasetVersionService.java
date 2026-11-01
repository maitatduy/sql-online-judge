package com.sqljudge.problem.service;

import com.sqljudge.problem.dto.response.StudioDatasetResponse;
import com.sqljudge.problem.entity.Dataset;
import com.sqljudge.problem.repository.DatasetRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudioDatasetVersionService {

    private final DatasetRepository datasetRepository;
    private final DatasetAccess datasetAccess;
    private final DatasetFreezer datasetFreezer;

    @Transactional
    public StudioDatasetResponse freeze(UUID datasetId) {
        Dataset dataset = datasetAccess.editable(datasetId);
        datasetFreezer.freezeLatest(dataset);
        return StudioDatasetResponse.from(datasetRepository.saveAndFlush(dataset));
    }
}
