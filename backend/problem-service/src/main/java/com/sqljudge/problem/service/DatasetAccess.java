package com.sqljudge.problem.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.common.security.CurrentUser;
import com.sqljudge.common.security.Role;
import com.sqljudge.common.security.SecurityUtils;
import com.sqljudge.problem.entity.Dataset;
import com.sqljudge.problem.entity.DatasetVariant;
import com.sqljudge.problem.entity.DatasetVersion;
import com.sqljudge.problem.repository.DatasetRepository;
import java.util.UUID;
import java.util.function.BiPredicate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DatasetAccess {

    private final DatasetRepository datasetRepository;

    public Dataset readable(UUID datasetId) {
        return find(datasetId, DatasetAccess::canRead);
    }

    public Dataset editable(UUID datasetId) {
        return find(datasetId, DatasetAccess::canEdit);
    }

    public DatasetVersion editableDraft(Dataset dataset) {
        if (!dataset.latestVersion().isDraft()) {
            dataset.addDraftFromLatest();
            datasetRepository.saveAndFlush(dataset);
        }
        return dataset.latestVersion();
    }

    public static DatasetVariant variantOf(DatasetVersion version, UUID variantId) {
        return version.findVariant(variantId)
                .orElseThrow(
                        () ->
                                new NotFoundException(
                                        ErrorCode.DATASET_VARIANT_NOT_FOUND,
                                        "Không tìm thấy bộ dữ liệu"));
    }

    private Dataset find(UUID datasetId, BiPredicate<Dataset, CurrentUser> allowed) {
        CurrentUser user = SecurityUtils.requireCurrentUser();
        return datasetRepository
                .findByIdAndDeletedAtIsNull(datasetId)
                .filter(dataset -> allowed.test(dataset, user))
                .orElseThrow(
                        () ->
                                new NotFoundException(
                                        ErrorCode.DATASET_NOT_FOUND, "Không tìm thấy database"));
    }

    private static boolean canEdit(Dataset dataset, CurrentUser user) {
        return dataset.getOwnerId().equals(user.id()) || user.hasRole(Role.ADMIN);
    }

    private static boolean canRead(Dataset dataset, CurrentUser user) {
        return canEdit(dataset, user) || user.hasRole(Role.MODERATOR);
    }
}
