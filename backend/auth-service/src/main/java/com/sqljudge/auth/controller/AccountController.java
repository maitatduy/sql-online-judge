package com.sqljudge.auth.controller;

import com.sqljudge.auth.dto.request.ChangePasswordRequest;
import com.sqljudge.auth.dto.request.UpdateAccountProfileRequest;
import com.sqljudge.auth.dto.response.AccountProfileResponse;
import com.sqljudge.auth.dto.response.UserAvatarResponse;
import com.sqljudge.auth.service.AccountService;
import com.sqljudge.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/account")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/profile")
    @Operation(summary = "Hồ sơ của tôi để chỉnh sửa")
    @ApiResponse(responseCode = "200", description = "Hồ sơ hiện tại")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    public AccountProfileResponse profile() {
        return accountService.profile(currentUserId());
    }

    @PatchMapping("/profile")
    @Operation(summary = "Sửa tên hiển thị, giới thiệu, trường, nơi sống, website")
    @ApiResponse(responseCode = "200", description = "Hồ sơ sau khi sửa")
    @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    public AccountProfileResponse updateProfile(
            @Valid @RequestBody UpdateAccountProfileRequest request) {
        return accountService.updateProfile(currentUserId(), request);
    }

    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Đổi mật khẩu; tài khoản đăng nhập bằng GitHub, Google thì đặt mật khẩu mới")
    @ApiResponse(responseCode = "204", description = "Đã đổi")
    @ApiResponse(responseCode = "400", description = "Mật khẩu hiện tại sai hoặc mật khẩu mới yếu")
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        accountService.changePassword(currentUserId(), request);
    }

    @PutMapping(path = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Đổi avatar của tôi, ảnh JPG, PNG hoặc WebP tối đa 2 MB")
    @ApiResponse(responseCode = "200", description = "URL ảnh mới")
    @ApiResponse(responseCode = "400", description = "Chưa chọn ảnh, sai định dạng hoặc quá 2 MB")
    @ApiResponse(responseCode = "503", description = "Kho ảnh tạm thời không nhận ảnh")
    public UserAvatarResponse replaceAvatar(
            @RequestParam(name = "file", required = false) MultipartFile file) {
        return accountService.replaceAvatar(currentUserId(), file);
    }

    @DeleteMapping("/avatar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Gỡ avatar của tôi")
    @ApiResponse(responseCode = "204", description = "Đã gỡ")
    public void removeAvatar() {
        accountService.removeAvatar(currentUserId());
    }

    private static UUID currentUserId() {
        return SecurityUtils.requireCurrentUser().id();
    }
}
