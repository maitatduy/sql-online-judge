package com.sqljudge.problem.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.ConflictException;
import com.sqljudge.common.security.SecurityUtils;
import com.sqljudge.common.web.PageResponse;
import com.sqljudge.problem.dto.request.CreateDatasetRequest;
import com.sqljudge.problem.dto.request.UpdateDatasetRequest;
import com.sqljudge.problem.dto.request.UpdateDatasetSchemaRequest;
import com.sqljudge.problem.dto.request.UpdateDatasetSchemaRequest.SchemaColumnRequest;
import com.sqljudge.problem.dto.request.UpdateDatasetSchemaRequest.SchemaTableRequest;
import com.sqljudge.problem.dto.response.StudioDatasetResponse;
import com.sqljudge.problem.dto.response.StudioDatasetSummaryResponse;
import com.sqljudge.problem.entity.Dataset;
import com.sqljudge.problem.entity.DatasetColumn;
import com.sqljudge.problem.entity.DatasetTable;
import com.sqljudge.problem.entity.DatasetVersion;
import com.sqljudge.problem.repository.DatasetRepository;
import com.sqljudge.problem.repository.ProblemRepository;
import java.time.Clock;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudioDatasetService {

    private static final int FIRST_VERSION = 1;

    private final DatasetRepository datasetRepository;
    private final DatasetAccess datasetAccess;
    private final ProblemRepository problemRepository;
    private final Clock clock;

    @Transactional
    public StudioDatasetResponse create(CreateDatasetRequest request) {
        String name = request.name().trim();
        Dataset dataset = new Dataset();
        dataset.setOwnerId(SecurityUtils.requireCurrentUser().id());
        dataset.setName(name);
        dataset.setSlug(uniqueSlug(SlugGenerator.slugOf(name)));
        dataset.setDescription(request.description() == null ? "" : request.description().trim());
        dataset.addVersion(FIRST_VERSION);
        return StudioDatasetResponse.from(datasetRepository.saveAndFlush(dataset));
    }

    @Transactional(readOnly = true)
    public PageResponse<StudioDatasetSummaryResponse> listOwned(
            String keyword, int page, int size) {
        return PageResponse.from(
                datasetRepository.findOwnedSummaries(
                        SecurityUtils.requireCurrentUser().id(),
                        LikePatterns.containingIgnoreCase(keyword),
                        PageRequest.of(page - 1, size)),
                StudioDatasetSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public StudioDatasetResponse get(UUID datasetId) {
        return StudioDatasetResponse.from(datasetAccess.readable(datasetId));
    }

    @Transactional
    public StudioDatasetResponse update(UUID datasetId, UpdateDatasetRequest request) {
        Dataset dataset = datasetAccess.editable(datasetId);
        if (request.name() != null) {
            dataset.setName(request.name().trim());
        }
        if (request.description() != null) {
            dataset.setDescription(request.description().trim());
        }
        return StudioDatasetResponse.from(datasetRepository.saveAndFlush(dataset));
    }

    @Transactional
    public void delete(UUID datasetId) {
        Dataset dataset = datasetAccess.editable(datasetId);
        long usingProblems = problemRepository.countUsingDataset(datasetId);
        if (usingProblems > 0) {
            throw new ConflictException(
                    ErrorCode.DATASET_IN_USE,
                    "Dataset đang được " + usingProblems + " bài dùng nên chưa xoá được");
        }
        dataset.setDeletedAt(clock.instant());
        datasetRepository.saveAndFlush(dataset);
    }

    @Transactional
    public StudioDatasetResponse replaceSchema(UUID datasetId, UpdateDatasetSchemaRequest request) {
        Dataset dataset = datasetAccess.editable(datasetId);
        DatasetVersion draft = datasetAccess.editableDraft(dataset);
        DatasetSchemaValidator.validate(request);
        draft.getTables().clear();
        datasetRepository.flush();
        request.tables().forEach(table -> addTable(draft, table));
        Set<String> tableNames =
                request.tables().stream().map(SchemaTableRequest::name).collect(Collectors.toSet());
        draft.getVariants().forEach(variant -> variant.keepTablesNamed(tableNames));
        return StudioDatasetResponse.from(datasetRepository.saveAndFlush(dataset));
    }

    private static void addTable(DatasetVersion draft, SchemaTableRequest request) {
        DatasetTable table = draft.addTable(request.name());
        for (SchemaColumnRequest columnRequest : request.columns()) {
            DatasetColumn column = table.addColumn(columnRequest.name(), columnRequest.type());
            column.setPrimaryKey(columnRequest.isPrimaryKey());
            column.setNullable(columnRequest.isNullable());
            if (columnRequest.references() != null) {
                column.setReferencesTable(columnRequest.references().table());
                column.setReferencesColumn(columnRequest.references().column());
            }
        }
    }

    private String uniqueSlug(String base) {
        return SlugGenerator.firstFree(
                base, new HashSet<>(datasetRepository.findSlugsLike(base, base + "-%")));
    }
}
