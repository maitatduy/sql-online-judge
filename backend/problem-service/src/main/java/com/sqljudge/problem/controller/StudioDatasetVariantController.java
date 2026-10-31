package com.sqljudge.problem.controller;

import com.sqljudge.problem.dto.request.CreateDatasetVariantRequest;
import com.sqljudge.problem.dto.request.UpdateDatasetVariantRequest;
import com.sqljudge.problem.dto.request.UploadVariantCsvRequest;
import com.sqljudge.problem.dto.response.StudioDatasetResponse;
import com.sqljudge.problem.dto.response.VariantCsvResponse;
import com.sqljudge.problem.service.StudioDatasetVariantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/studio/datasets/{datasetId}/variants")
@PreAuthorize("hasAnyRole('SETTER', 'ADMIN')")
@RequiredArgsConstructor
public class StudioDatasetVariantController {

    private final StudioDatasetVariantService variantService;

    @PostMapping
    @Operation(summary = "Thêm bộ dữ liệu vào bản nháp của dataset")
    @ApiResponse(responseCode = "201", description = "Dataset kèm bộ dữ liệu vừa thêm")
    @ApiResponse(responseCode = "400", description = "Tên trống hoặc quá dài")
    @ApiResponse(responseCode = "404", description = "Không có dataset hoặc không được sửa")
    @ApiResponse(responseCode = "409", description = "Tên bộ dữ liệu đã được dùng")
    @ApiResponse(responseCode = "422", description = "Phiên bản mới nhất đã chốt")
    public ResponseEntity<StudioDatasetResponse> addVariant(
            @PathVariable UUID datasetId, @Valid @RequestBody CreateDatasetVariantRequest request) {
        StudioDatasetResponse dataset = variantService.addVariant(datasetId, request);
        UUID variantId = dataset.variants().get(dataset.variants().size() - 1).id();
        return ResponseEntity.created(
                        URI.create("/api/studio/datasets/" + datasetId + "/variants/" + variantId))
                .body(dataset);
    }

    @PatchMapping("/{variantId}")
    @Operation(summary = "Đổi tên, đổi công khai hay ẩn; trường không gửi lên giữ nguyên")
    @ApiResponse(responseCode = "200", description = "Dataset sau khi sửa")
    @ApiResponse(responseCode = "404", description = "Không có dataset hoặc bộ dữ liệu")
    @ApiResponse(responseCode = "409", description = "Tên bộ dữ liệu đã được dùng")
    @ApiResponse(responseCode = "422", description = "Phiên bản mới nhất đã chốt")
    public StudioDatasetResponse updateVariant(
            @PathVariable UUID datasetId,
            @PathVariable UUID variantId,
            @Valid @RequestBody UpdateDatasetVariantRequest request) {
        return variantService.updateVariant(datasetId, variantId, request);
    }

    @DeleteMapping("/{variantId}")
    @Operation(summary = "Xoá bộ dữ liệu cùng CSV của nó")
    @ApiResponse(responseCode = "204", description = "Đã xoá")
    @ApiResponse(responseCode = "404", description = "Không có dataset hoặc bộ dữ liệu")
    @ApiResponse(responseCode = "422", description = "Phiên bản mới nhất đã chốt")
    public ResponseEntity<Void> deleteVariant(
            @PathVariable UUID datasetId, @PathVariable UUID variantId) {
        variantService.deleteVariant(datasetId, variantId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{variantId}/tables/{tableName}")
    @Operation(summary = "Nhập CSV cho một bảng của bộ dữ liệu, thay dữ liệu cũ nếu có")
    @ApiResponse(responseCode = "200", description = "Dataset với số dòng mới của bảng")
    @ApiResponse(responseCode = "400", description = "CSV sai hoặc bảng không có trong schema")
    @ApiResponse(responseCode = "404", description = "Không có dataset hoặc bộ dữ liệu")
    @ApiResponse(responseCode = "422", description = "Phiên bản mới nhất đã chốt")
    public StudioDatasetResponse uploadCsv(
            @PathVariable UUID datasetId,
            @PathVariable UUID variantId,
            @PathVariable String tableName,
            @Valid @RequestBody UploadVariantCsvRequest request) {
        return variantService.uploadCsv(datasetId, variantId, tableName, request);
    }

    @GetMapping("/{variantId}/tables/{tableName}")
    @PreAuthorize("hasAnyRole('SETTER', 'ADMIN', 'MODERATOR')")
    @Operation(summary = "Đọc CSV của một bảng trong bộ dữ liệu")
    @ApiResponse(responseCode = "200", description = "Nội dung CSV và số dòng")
    @ApiResponse(responseCode = "404", description = "Không có dataset, bộ dữ liệu hoặc CSV")
    public VariantCsvResponse getCsv(
            @PathVariable UUID datasetId,
            @PathVariable UUID variantId,
            @PathVariable String tableName) {
        return variantService.getCsv(datasetId, variantId, tableName);
    }
}
