package com.sqljudge.learning.controller;

import com.sqljudge.learning.dto.request.CellRequest;
import com.sqljudge.learning.dto.response.StudioCellResponse;
import com.sqljudge.learning.service.StudioCellService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAnyRole('SETTER', 'ADMIN')")
@RequiredArgsConstructor
public class StudioCellController {

    private final StudioCellService studioCellService;

    @GetMapping("/api/studio/lessons/{lessonId}/cells")
    @Operation(summary = "Các ô SQL của bài giảng đang soạn")
    @ApiResponse(responseCode = "200", description = "Ô SQL theo thứ tự")
    @ApiResponse(responseCode = "404", description = "Không có bài hoặc không phải của người gọi")
    public List<StudioCellResponse> list(@PathVariable UUID lessonId) {
        return studioCellService.list(lessonId);
    }

    @PostMapping("/api/studio/lessons/{lessonId}/cells")
    @Operation(summary = "Thêm ô SQL chạy trên một bộ dữ liệu công khai đã chốt")
    @ApiResponse(responseCode = "201", description = "Ô SQL vừa thêm")
    @ApiResponse(
            responseCode = "422",
            description = "Bộ dữ liệu ẩn, chưa chốt, không tồn tại hoặc khoá đang chờ duyệt")
    @ApiResponse(responseCode = "503", description = "Chưa kiểm tra được bộ dữ liệu")
    public ResponseEntity<StudioCellResponse> add(
            @PathVariable UUID lessonId, @Valid @RequestBody CellRequest request) {
        StudioCellResponse cell = studioCellService.add(lessonId, request);
        return ResponseEntity.created(URI.create("/api/studio/cells/" + cell.id())).body(cell);
    }

    @PatchMapping("/api/studio/cells/{cellId}")
    @Operation(summary = "Sửa câu SQL mở đầu, dialect hoặc bộ dữ liệu của ô")
    @ApiResponse(responseCode = "200", description = "Ô SQL sau khi sửa")
    @ApiResponse(responseCode = "422", description = "Bộ dữ liệu không dùng được hoặc khoá bị khoá")
    public StudioCellResponse update(
            @PathVariable UUID cellId, @Valid @RequestBody CellRequest request) {
        return studioCellService.update(cellId, request);
    }

    @DeleteMapping("/api/studio/cells/{cellId}")
    @Operation(summary = "Xoá ô SQL")
    @ApiResponse(responseCode = "204", description = "Đã xoá")
    @ApiResponse(responseCode = "422", description = "Khoá đang chờ duyệt")
    public ResponseEntity<Void> delete(@PathVariable UUID cellId) {
        studioCellService.delete(cellId);
        return ResponseEntity.noContent().build();
    }
}
