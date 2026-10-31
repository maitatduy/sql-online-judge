package com.sqljudge.problem.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.BusinessRuleException;
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
        DatasetVersion draft = datasetAccess.editableDraft(dataset);
        DatasetReadiness.check(draft);
        draft.setChecksum(DatasetChecksum.of(draft));
        draft.setFrozenAt(clock.instant());
        return StudioDatasetResponse.from(datasetRepository.saveAndFlush(dataset));
    }

    @Transactional
    public StudioDatasetResponse createDraft(UUID datasetId) {
        Dataset dataset = datasetAccess.editable(datasetId);
        if (dataset.latestVersion().isDraft()) {
            throw new BusinessRuleException(
                    ErrorCode.DATASET_DRAFT_EXISTS,
                    "Dataset đang có bản nháp, hãy sửa tiếp bản nháp đó");
        }
        dataset.addDraftFromLatest();
        return StudioDatasetResponse.from(datasetRepository.saveAndFlush(dataset));
    }
}
