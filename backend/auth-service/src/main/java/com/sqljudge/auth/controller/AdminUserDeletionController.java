package com.sqljudge.auth.controller;

import com.sqljudge.auth.dto.request.DeleteUserRequest;
import com.sqljudge.auth.service.AdminUserDeletionService;
import com.sqljudge.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users/{username}")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserDeletionController {

    private final AdminUserDeletionService adminUserDeletionService;

    @Operation(
            summary = "Xoá mềm tài khoản: ẩn khỏi danh sách, đăng xuất mọi phiên, ghi nhật ký",
            description = "Username và email vẫn được giữ để khôi phục được về sau.")
    @ApiResponse(responseCode = "204", description = "Đã xoá")
    @ApiResponse(responseCode = "400", description = "Thiếu lý do hoặc lý do quá dài")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(responseCode = "403", description = "Không phải ADMIN")
    @ApiResponse(responseCode = "404", description = "Không có tài khoản hoặc đã bị xoá")
    @ApiResponse(responseCode = "422", description = "Admin tự xoá chính mình")
    @DeleteMapping
    public ResponseEntity<Void> delete(
            @PathVariable String username, @Valid @RequestBody DeleteUserRequest request) {
        adminUserDeletionService.delete(username, request, SecurityUtils.requireCurrentUser().id());
        return ResponseEntity.noContent().build();
    }
}
