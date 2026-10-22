package com.sqljudge.auth.controller;

import com.sqljudge.auth.dto.request.UpdateRestrictionsRequest;
import com.sqljudge.auth.dto.response.UserRestrictionsResponse;
import com.sqljudge.auth.service.AdminUserRestrictionService;
import com.sqljudge.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users/{username}/restrictions")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserRestrictionController {

    private final AdminUserRestrictionService adminUserRestrictionService;

    @Operation(
            summary = "Hạn chế của một tài khoản: hạn mức AI mỗi ngày, cấm contest có rating",
            description =
                    "Hạn chế hiện chỉ được lưu ở auth-service, sẽ được áp dụng khi có ai-service"
                            + " và contest-service.")
    @ApiResponse(responseCode = "200", description = "Hạn chế hiện tại, mặc định nếu chưa đặt")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(responseCode = "403", description = "Không phải ADMIN")
    @ApiResponse(responseCode = "404", description = "Không có người dùng với username này")
    @GetMapping
    public UserRestrictionsResponse get(@PathVariable String username) {
        return adminUserRestrictionService.get(username);
    }

    @Operation(
            summary = "Sửa một phần hạn chế, bắt buộc có lý do, ghi nhật ký giá trị trước và sau",
            description =
                    "Gửi đúng giá trị đang có thì trả 200 với trạng thái hiện tại và không ghi"
                            + " nhật ký. Tài khoản chưa từng chỉnh hạn mức AI theo hạn mức mặc"
                            + " định của hệ thống. Hạn chế hiện chỉ được lưu ở auth-service, sẽ"
                            + " được áp dụng khi có ai-service và contest-service.")
    @ApiResponse(responseCode = "200", description = "Hạn chế sau khi sửa")
    @ApiResponse(
            responseCode = "400",
            description = "Không có thay đổi, hạn mức sai hoặc thiếu lý do")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(responseCode = "403", description = "Không phải ADMIN")
    @ApiResponse(responseCode = "404", description = "Không có tài khoản hoặc đã bị xoá")
    @ApiResponse(responseCode = "409", description = "Admin khác vừa sửa cùng lúc")
    @ApiResponse(responseCode = "422", description = "Admin tự đặt hạn chế cho chính mình")
    @PatchMapping
    public UserRestrictionsResponse update(
            @PathVariable String username, @Valid @RequestBody UpdateRestrictionsRequest request) {
        return adminUserRestrictionService.update(
                username, request, SecurityUtils.requireCurrentUser().id());
    }
}
