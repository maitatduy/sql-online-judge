package com.sqljudge.problem.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.problem.dto.response.InternalDatasetSnapshotResponse;
import com.sqljudge.problem.dto.response.InternalDatasetSnapshotResponse.ColumnReferenceResponse;
import com.sqljudge.problem.dto.response.InternalDatasetSnapshotResponse.SnapshotColumnResponse;
import com.sqljudge.problem.dto.response.InternalDatasetSnapshotResponse.SnapshotTableResponse;
import com.sqljudge.problem.dto.response.InternalDatasetSnapshotResponse.SnapshotVariantResponse;
import com.sqljudge.problem.dto.response.InternalDatasetSnapshotResponse.SnapshotVariantTableResponse;
import com.sqljudge.problem.entity.DatasetColumn;
import com.sqljudge.problem.entity.DatasetTable;
import com.sqljudge.problem.entity.DatasetVariant;
import com.sqljudge.problem.entity.DatasetVersion;
import com.sqljudge.problem.repository.DatasetVersionRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InternalDatasetSnapshotService {

    private final DatasetVersionRepository datasetVersionRepository;

    @Transactional(readOnly = true)
    public InternalDatasetSnapshotResponse snapshot(UUID versionId) {
        DatasetVersion version =
                datasetVersionRepository
                        .findByIdAndFrozenAtIsNotNull(versionId)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                ErrorCode.DATASET_NOT_FOUND,
                                                "Không tìm thấy bản database đã chốt"));
        return new InternalDatasetSnapshotResponse(
                version.getId(),
                version.getChecksum(),
                version.getTables().stream().map(InternalDatasetSnapshotService::tableOf).toList(),
                version.getVariants().stream()
                        .map(variant -> variantOf(variant, version.getTables()))
                        .toList());
    }

    private static SnapshotTableResponse tableOf(DatasetTable table) {
        return new SnapshotTableResponse(
                table.getName(),
                table.getColumns().stream().map(InternalDatasetSnapshotService::columnOf).toList());
    }

    private static SnapshotColumnResponse columnOf(DatasetColumn column) {
        ColumnReferenceResponse references =
                column.getReferencesTable() == null
                        ? null
                        : new ColumnReferenceResponse(
                                column.getReferencesTable(), column.getReferencesColumn());
        return new SnapshotColumnResponse(
                column.getName(),
                column.getLogicalType(),
                column.isPrimaryKey(),
                column.isNullable(),
                references);
    }

    private static SnapshotVariantResponse variantOf(
            DatasetVariant variant, List<DatasetTable> tables) {
        List<SnapshotVariantTableResponse> data =
                tables.stream().flatMap(table -> dataOf(variant, table).stream()).toList();
        return new SnapshotVariantResponse(
                variant.getId(), variant.getName(), variant.isPublic(), data);
    }

    private static Optional<SnapshotVariantTableResponse> dataOf(
            DatasetVariant variant, DatasetTable table) {
        return variant.findTable(table.getName())
                .map(
                        variantTable ->
                                new SnapshotVariantTableResponse(
                                        table.getName(),
                                        CsvTableParser.parse(
                                                        variantTable.getCsv(),
                                                        CsvTableParser.columnsOf(table))
                                                .rows()));
    }
}
