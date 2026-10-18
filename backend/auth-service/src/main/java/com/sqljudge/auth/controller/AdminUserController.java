package com.sqljudge.auth.controller;

import com.sqljudge.auth.dto.request.CreateUserRequest;
import com.sqljudge.auth.dto.response.AccountStatus;
import com.sqljudge.auth.dto.response.AdminUserDetailResponse;
import com.sqljudge.auth.dto.response.AdminUserSummaryResponse;
import com.sqljudge.auth.dto.response.UserCountsResponse;
import com.sqljudge.auth.service.AdminUserCommandService;
import com.sqljudge.auth.service.AdminUserQueryService;
import com.sqljudge.auth.service.AdminUserSearch;
import com.sqljudge.common.security.Role;
import com.sqljudge.common.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_KEYWORD_LENGTH = 254;

    private final AdminUserQueryService adminUserQueryService;
    private final AdminUserCommandService adminUserCommandService;

    @Operation(summary = "Danh sách người dùng cho admin: lọc, tìm, phân trang")
    @ApiResponse(responseCode = "200", description = "Một trang người dùng, mới tạo đứng trước")
    @ApiResponse(responseCode = "400", description = "Tham số lọc hoặc phân trang không hợp lệ")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(responseCode = "403", description = "Không phải ADMIN")
    @GetMapping("/users")
    public PageResponse<AdminUserSummaryResponse> listUsers(
            @RequestParam(required = false)
                    @Size(max = MAX_KEYWORD_LENGTH, message = "Từ khoá quá dài") String q,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(required = false) Boolean emailVerified,
            @RequestParam(defaultValue = "false") boolean deleted,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "Phải từ 1 trở lên") int page,
            @RequestParam(defaultValue = "20")
                    @Min(value = 1, message = "Phải từ 1 đến 100") @Max(value = MAX_PAGE_SIZE, message = "Phải từ 1 đến 100") int size) {
        return adminUserQueryService.search(
                new AdminUserSearch(q, role, status, emailVerified, deleted), page, size);
    }

    @Operation(summary = "Admin tạo tài khoản và gửi email kích hoạt để người dùng tự đặt mật khẩu")
    @ApiResponse(responseCode = "201", description = "Đã tạo, header Location trỏ tới chi tiết")
    @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(responseCode = "403", description = "Không phải ADMIN")
    @ApiResponse(responseCode = "409", description = "Username hoặc email đã được dùng")
    @PostMapping("/users")
    public ResponseEntity<AdminUserDetailResponse> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        AdminUserDetailResponse created = adminUserCommandService.createUser(request);
        URI location =
                UriComponentsBuilder.fromPath("/api/admin/users/{username}")
                        .buildAndExpand(created.account().username())
                        .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "Chi tiết một người dùng cho admin, kể cả tài khoản đã xoá")
    @ApiResponse(responseCode = "200", description = "Thông tin tài khoản và hồ sơ")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(responseCode = "403", description = "Không phải ADMIN")
    @ApiResponse(responseCode = "404", description = "Không có người dùng với username này")
    @GetMapping("/users/{username}")
    public AdminUserDetailResponse getUser(@PathVariable String username) {
        return adminUserQueryService.getUser(username);
    }

    @Operation(summary = "Số liệu tóm tắt người dùng cho admin")
    @ApiResponse(responseCode = "200", description = "Số tài khoản theo nhóm, không tính đã xoá")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(responseCode = "403", description = "Không phải ADMIN")
    @GetMapping("/user-summary")
    public UserCountsResponse userSummary() {
        return adminUserQueryService.countUsers();
    }
}
