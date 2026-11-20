package com.sqljudge.learning.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.BusinessRuleException;
import com.sqljudge.learning.client.DatasetVariantInfo;
import com.sqljudge.learning.client.ProblemServiceClient;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LessonDatasetResolver {

    private final ProblemServiceClient problemServiceClient;

    public LessonDataset resolve(UUID variantId) {
        DatasetVariantInfo variant = problemServiceClient.datasetVariant(variantId);
        if (!variant.isPublic()) {
            throw new BusinessRuleException(
                    ErrorCode.DATASET_VARIANT_NOT_PUBLIC,
                    "Chỉ gắn được bộ dữ liệu công khai vào bài giảng");
        }
        if (!variant.isFrozen()) {
            throw new BusinessRuleException(
                    ErrorCode.DATASET_NOT_FROZEN,
                    "Hãy chốt phiên bản database trước khi gắn vào bài giảng");
        }
        return new LessonDataset(variant.variantId(), variant.versionId(), variant.checksum());
    }
}
