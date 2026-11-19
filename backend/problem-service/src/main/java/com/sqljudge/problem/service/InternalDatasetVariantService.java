package com.sqljudge.problem.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.problem.dto.response.InternalDatasetVariantResponse;
import com.sqljudge.problem.repository.DatasetVariantRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InternalDatasetVariantService {

    private final DatasetVariantRepository datasetVariantRepository;

    @Transactional(readOnly = true)
    public InternalDatasetVariantResponse describe(UUID variantId) {
        return datasetVariantRepository
                .findLiveWithDataset(variantId)
                .map(InternalDatasetVariantResponse::of)
                .orElseThrow(
                        () ->
                                new NotFoundException(
                                        ErrorCode.DATASET_VARIANT_NOT_FOUND,
                                        "Không tìm thấy bộ dữ liệu"));
    }
}
