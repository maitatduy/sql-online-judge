package com.sqljudge.auth.controller;

import com.sqljudge.auth.dto.response.UserAvatarResponse;
import com.sqljudge.auth.service.AdminUserAvatarService;
import com.sqljudge.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/users/{username}/avatar")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserAvatarController {

    private final AdminUserAvatarService adminUserAvatarService;

    @Operation(summary = "Đổi avatar: PNG, JPG hoặc WEBP tối đa 2 MB, ảnh cũ bị xoá sau khi lưu")
    @ApiResponse(responseCode = "200", description = "URL ảnh mới")
    @ApiResponse(responseCode = "400", description = "Chưa chọn ảnh, sai định dạng hoặc quá 2 MB")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(responseCode = "403", description = "Không phải ADMIN")
    @ApiResponse(responseCode = "404", description = "Không có tài khoản hoặc đã bị xoá")
    @ApiResponse(responseCode = "503", description = "Kho ảnh tạm thời không nhận ảnh")
    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserAvatarResponse replace(
            @PathVariable String username,
            @RequestParam(name = "file", required = false) MultipartFile file) {
        return adminUserAvatarService.replace(
                username, file, SecurityUtils.requireCurrentUser().id());
    }

    @Operation(summary = "Gỡ avatar: đặt về ảnh mặc định, xoá ảnh khỏi kho sau khi lưu")
    @ApiResponse(responseCode = "204", description = "Đã gỡ, hoặc tài khoản chưa có avatar")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(responseCode = "403", description = "Không phải ADMIN")
    @ApiResponse(responseCode = "404", description = "Không có tài khoản hoặc đã bị xoá")
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable String username) {
        adminUserAvatarService.remove(username, SecurityUtils.requireCurrentUser().id());
    }
}
