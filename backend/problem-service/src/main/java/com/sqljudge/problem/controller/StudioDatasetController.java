package com.sqljudge.problem.controller;

import com.sqljudge.common.web.PageResponse;
import com.sqljudge.problem.dto.request.CreateDatasetRequest;
import com.sqljudge.problem.dto.request.UpdateDatasetRequest;
import com.sqljudge.problem.dto.request.UpdateDatasetSchemaRequest;
import com.sqljudge.problem.dto.response.StudioDatasetResponse;
import com.sqljudge.problem.dto.response.StudioDatasetSummaryResponse;
import com.sqljudge.problem.service.StudioDatasetService;
import com.sqljudge.problem.service.StudioDatasetVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/studio/datasets")
@PreAuthorize("hasAnyRole('SETTER', 'ADMIN')")
@RequiredArgsConstructor
public class StudioDatasetController {

    private static final String PATH = "/api/studio/datasets";
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_KEYWORD_LENGTH = 100;

    private final StudioDatasetService studioDatasetService;
    private final StudioDatasetVersionService studioDatasetVersionService;

    @PostMapping
    @Operation(summary = "Tạo dataset mới kèm phiên bản 1 dạng nháp, slug sinh từ tên")
    @ApiResponse(responseCode = "201", description = "Dataset vừa tạo, kèm header Location")
    @ApiResponse(responseCode = "400", description = "Tên trống hoặc quá dài")
    @ApiResponse(responseCode = "403", description = "Không phải SETTER hoặc ADMIN")
    public ResponseEntity<StudioDatasetResponse> create(
            @Valid @RequestBody CreateDatasetRequest request) {
        StudioDatasetResponse created = studioDatasetService.create(request);
        return ResponseEntity.created(URI.create(PATH + "/" + created.id())).body(created);
    }

    @GetMapping
    @Operation(summary = "Dataset của tôi: tìm theo tên, mới sửa nhất trước")
    @ApiResponse(responseCode = "200", description = "Một trang dataset do chính người gọi tạo")
    @ApiResponse(responseCode = "400", description = "Tham số tìm kiếm hoặc phân trang sai")
    @ApiResponse(responseCode = "403", description = "Không phải SETTER hoặc ADMIN")
    public PageResponse<StudioDatasetSummaryResponse> listOwned(
            @RequestParam(required = false)
                    @Size(max = MAX_KEYWORD_LENGTH, message = "Từ khoá quá dài") String q,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "Phải từ 1 trở lên") int page,
            @RequestParam(defaultValue = "20")
                    @Min(value = 1, message = "Phải từ 1 đến 100") @Max(value = MAX_PAGE_SIZE, message = "Phải từ 1 đến 100") int size) {
        return studioDatasetService.listOwned(q, page, size);
    }

    @GetMapping("/{datasetId}")
    @PreAuthorize("hasAnyRole('SETTER', 'ADMIN', 'MODERATOR')")
    @Operation(summary = "Đọc dataset: thông tin, các phiên bản, schema của phiên bản mới nhất")
    @ApiResponse(
            responseCode = "200",
            description = "Dataset của người gọi, hoặc bất kỳ nếu là ADMIN hay MODERATOR")
    @ApiResponse(responseCode = "404", description = "Không có dataset hoặc không được xem")
    public StudioDatasetResponse get(@PathVariable UUID datasetId) {
        return studioDatasetService.get(datasetId);
    }

    @PatchMapping("/{datasetId}")
    @Operation(summary = "Sửa tên và mô tả; trường không gửi lên giữ nguyên")
    @ApiResponse(responseCode = "200", description = "Dataset sau khi sửa")
    @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    @ApiResponse(responseCode = "404", description = "Không có dataset hoặc không được sửa")
    public StudioDatasetResponse update(
            @PathVariable UUID datasetId, @Valid @RequestBody UpdateDatasetRequest request) {
        return studioDatasetService.update(datasetId, request);
    }

    @PutMapping("/{datasetId}/schema")
    @Operation(summary = "Thay toàn bộ schema của bản nháp")
    @ApiResponse(responseCode = "200", description = "Dataset với schema mới")
    @ApiResponse(responseCode = "400", description = "Schema sai, kèm đường dẫn tới trường lỗi")
    @ApiResponse(responseCode = "404", description = "Không có dataset hoặc không được sửa")
    @ApiResponse(responseCode = "422", description = "Phiên bản mới nhất đã chốt")
    public StudioDatasetResponse replaceSchema(
            @PathVariable UUID datasetId, @Valid @RequestBody UpdateDatasetSchemaRequest request) {
        return studioDatasetService.replaceSchema(datasetId, request);
    }

    @PostMapping("/{datasetId}/freeze")
    @Operation(summary = "Chốt bản nháp: kiểm tra đủ dữ liệu và khoá ngoại, ghi checksum")
    @ApiResponse(responseCode = "200", description = "Dataset với phiên bản vừa chốt")
    @ApiResponse(responseCode = "404", description = "Không có dataset hoặc không được sửa")
    @ApiResponse(
            responseCode = "422",
            description = "Bản nháp chưa đủ điều kiện, hoặc phiên bản mới nhất đã chốt")
    public StudioDatasetResponse freeze(@PathVariable UUID datasetId) {
        return studioDatasetVersionService.freeze(datasetId);
    }

    @PostMapping("/{datasetId}/versions")
    @Operation(summary = "Tạo bản nháp mới, sao chép từ phiên bản đã chốt mới nhất")
    @ApiResponse(responseCode = "201", description = "Dataset với bản nháp mới")
    @ApiResponse(responseCode = "404", description = "Không có dataset hoặc không được sửa")
    @ApiResponse(responseCode = "422", description = "Dataset đang có bản nháp")
    public ResponseEntity<StudioDatasetResponse> createDraft(@PathVariable UUID datasetId) {
        StudioDatasetResponse dataset = studioDatasetVersionService.createDraft(datasetId);
        return ResponseEntity.created(URI.create(PATH + "/" + datasetId)).body(dataset);
    }
}
