package com.sqljudge.auth.controller;

import com.sqljudge.auth.dto.response.UserSessionsResponse;
import com.sqljudge.auth.service.AdminUserSessionService;
import com.sqljudge.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users/{username}/sessions")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserSessionController {

    private final AdminUserSessionService adminUserSessionService;

    @Operation(summary = "Các phiên đăng nhập còn hiệu lực của một người dùng")
    @ApiResponse(responseCode = "200", description = "Phiên dùng gần nhất đứng trước")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(responseCode = "403", description = "Không phải ADMIN")
    @ApiResponse(responseCode = "404", description = "Không có người dùng")
    @GetMapping
    public UserSessionsResponse listSessions(@PathVariable String username) {
        return adminUserSessionService.listSessions(username);
    }

    @Operation(summary = "Đăng xuất một phiên của người dùng, ghi nhật ký")
    @ApiResponse(responseCode = "204", description = "Đã đăng xuất phiên")
    @ApiResponse(responseCode = "400", description = "Mã phiên không hợp lệ")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(responseCode = "403", description = "Không phải ADMIN")
    @ApiResponse(responseCode = "404", description = "Không có người dùng hoặc phiên")
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> revokeSession(
            @PathVariable String username, @PathVariable UUID sessionId) {
        adminUserSessionService.revokeSession(
                username, sessionId, SecurityUtils.requireCurrentUser().id());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Đăng xuất mọi phiên của người dùng, ghi nhật ký")
    @ApiResponse(responseCode = "204", description = "Đã đăng xuất mọi phiên")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(responseCode = "403", description = "Không phải ADMIN")
    @ApiResponse(responseCode = "404", description = "Không có người dùng")
    @DeleteMapping
    public ResponseEntity<Void> revokeAllSessions(@PathVariable String username) {
        adminUserSessionService.revokeAllSessions(
                username, SecurityUtils.requireCurrentUser().id());
        return ResponseEntity.noContent().build();
    }
}
