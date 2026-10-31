package com.sqljudge.problem.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.ConflictException;
import com.sqljudge.common.exception.InvalidFieldException;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.problem.dto.request.CreateDatasetVariantRequest;
import com.sqljudge.problem.dto.request.UpdateDatasetVariantRequest;
import com.sqljudge.problem.dto.request.UploadVariantCsvRequest;
import com.sqljudge.problem.dto.response.StudioDatasetResponse;
import com.sqljudge.problem.dto.response.VariantCsvResponse;
import com.sqljudge.problem.entity.Dataset;
import com.sqljudge.problem.entity.DatasetTable;
import com.sqljudge.problem.entity.DatasetVariant;
import com.sqljudge.problem.entity.DatasetVersion;
import com.sqljudge.problem.repository.DatasetRepository;
import com.sqljudge.problem.service.CsvTableParser.CsvColumn;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudioDatasetVariantService {

    private final DatasetRepository datasetRepository;
    private final DatasetAccess datasetAccess;

    @Transactional
    public StudioDatasetResponse addVariant(UUID datasetId, CreateDatasetVariantRequest request) {
        Dataset dataset = datasetAccess.editable(datasetId);
        DatasetVersion draft = datasetAccess.editableDraft(dataset);
        String name = request.name().trim();
        requireFreeName(draft, name, null);
        draft.addVariant(name, request.isPublic());
        return StudioDatasetResponse.from(datasetRepository.saveAndFlush(dataset));
    }

    @Transactional
    public StudioDatasetResponse updateVariant(
            UUID datasetId, UUID variantId, UpdateDatasetVariantRequest request) {
        Dataset dataset = datasetAccess.editable(datasetId);
        DatasetVersion draft = datasetAccess.editableDraft(dataset);
        DatasetVariant variant = DatasetAccess.variantOf(draft, variantId);
        if (request.name() != null) {
            String name = request.name().trim();
            requireFreeName(draft, name, variantId);
            variant.setName(name);
        }
        if (request.isPublic() != null) {
            variant.setPublic(request.isPublic());
        }
        return StudioDatasetResponse.from(datasetRepository.saveAndFlush(dataset));
    }

    @Transactional
    public void deleteVariant(UUID datasetId, UUID variantId) {
        Dataset dataset = datasetAccess.editable(datasetId);
        DatasetVersion draft = datasetAccess.editableDraft(dataset);
        draft.getVariants().remove(DatasetAccess.variantOf(draft, variantId));
        datasetRepository.saveAndFlush(dataset);
    }

    @Transactional
    public StudioDatasetResponse uploadCsv(
            UUID datasetId, UUID variantId, String tableName, UploadVariantCsvRequest request) {
        Dataset dataset = datasetAccess.editable(datasetId);
        DatasetVersion draft = datasetAccess.editableDraft(dataset);
        DatasetVariant variant = DatasetAccess.variantOf(draft, variantId);
        DatasetTable table =
                draft.findTable(tableName)
                        .orElseThrow(
                                () ->
                                        new InvalidFieldException(
                                                "tableName", "Bảng không có trong schema"));
        int rowCount = CsvTableParser.parse(request.csv(), columnsOf(table)).rowCount();
        variant.putTable(tableName, request.csv(), rowCount);
        return StudioDatasetResponse.from(datasetRepository.saveAndFlush(dataset));
    }

    @Transactional(readOnly = true)
    public VariantCsvResponse getCsv(UUID datasetId, UUID variantId, String tableName) {
        DatasetVersion latest = datasetAccess.readable(datasetId).latestVersion();
        return DatasetAccess.variantOf(latest, variantId)
                .findTable(tableName)
                .map(VariantCsvResponse::from)
                .orElseThrow(
                        () ->
                                new NotFoundException(
                                        ErrorCode.DATASET_VARIANT_NOT_FOUND,
                                        "Bộ dữ liệu chưa có CSV cho bảng này"));
    }

    private static List<CsvColumn> columnsOf(DatasetTable table) {
        return table.getColumns().stream()
                .map(
                        column ->
                                new CsvColumn(
                                        column.getName(),
                                        column.getLogicalType(),
                                        column.isPrimaryKey(),
                                        column.isNullable()))
                .toList();
    }

    private static void requireFreeName(DatasetVersion draft, String name, UUID exceptVariantId) {
        if (draft.hasVariantNamed(name, exceptVariantId)) {
            throw new ConflictException(
                    ErrorCode.DATASET_VARIANT_NAME_TAKEN, "Tên bộ dữ liệu đã được dùng");
        }
    }
}
