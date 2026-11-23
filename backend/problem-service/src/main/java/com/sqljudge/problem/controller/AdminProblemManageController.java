package com.sqljudge.problem.controller;

import com.sqljudge.problem.dto.request.QuickEditProblemRequest;
import com.sqljudge.problem.dto.request.TransferOwnershipRequest;
import com.sqljudge.problem.service.AdminProblemActions;
import com.sqljudge.problem.service.AdminProblemCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/problems/{problemId}")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminProblemManageController {

    private final AdminProblemCommandService commandService;
    private final AdminProblemActions problemActions;

    @PatchMapping
    @Operation(summary = "Sửa nhanh tên, độ khó và chủ đề của bài đã xuất bản")
    @ApiResponse(responseCode = "204", description = "Đã sửa")
    @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    @ApiResponse(responseCode = "404", description = "Không có bài đã xuất bản")
    @ApiResponse(responseCode = "422", description = "Có chủ đề không tồn tại")
    public ResponseEntity<Void> quickEdit(
            @PathVariable UUID problemId, @Valid @RequestBody QuickEditProblemRequest request) {
        commandService.quickEdit(problemId, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/hide")
    @Operation(summary = "Ẩn bài khỏi người học, bài nộp cũ vẫn giữ")
    @ApiResponse(responseCode = "204", description = "Đã ẩn")
    @ApiResponse(responseCode = "409", description = "Bài đang thuộc contest chưa kết thúc")
    @ApiResponse(responseCode = "422", description = "Bài đang ẩn")
    @ApiResponse(responseCode = "503", description = "Không kiểm tra được contest")
    public ResponseEntity<Void> hide(@PathVariable UUID problemId) {
        problemActions.hide(problemId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/restore")
    @Operation(summary = "Hiện lại bài về phạm vi trước khi ẩn")
    @ApiResponse(responseCode = "204", description = "Đã hiện lại")
    @ApiResponse(responseCode = "422", description = "Bài không bị ẩn")
    public ResponseEntity<Void> restore(@PathVariable UUID problemId) {
        commandService.restore(problemId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/transfer-ownership")
    @Operation(summary = "Chuyển bài sang một người ra đề khác")
    @ApiResponse(responseCode = "204", description = "Đã chuyển")
    @ApiResponse(responseCode = "404", description = "Không có bài hoặc người nhận")
    @ApiResponse(responseCode = "422", description = "Người nhận không phải người ra đề hoạt động")
    public ResponseEntity<Void> transferOwnership(
            @PathVariable UUID problemId, @Valid @RequestBody TransferOwnershipRequest request) {
        problemActions.transferOwnership(problemId, request.username());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Operation(summary = "Xoá mềm bài đã xuất bản")
    @ApiResponse(responseCode = "204", description = "Đã xoá")
    @ApiResponse(responseCode = "409", description = "Bài đang thuộc contest chưa kết thúc")
    @ApiResponse(responseCode = "503", description = "Không kiểm tra được contest")
    public ResponseEntity<Void> delete(@PathVariable UUID problemId) {
        problemActions.delete(problemId);
        return ResponseEntity.noContent().build();
    }
}
