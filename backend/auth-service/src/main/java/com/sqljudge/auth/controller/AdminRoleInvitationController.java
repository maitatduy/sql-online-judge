package com.sqljudge.auth.controller;

import com.sqljudge.auth.dto.request.InviteRolesRequest;
import com.sqljudge.auth.dto.response.InviteRolesResponse;
import com.sqljudge.auth.service.RoleInvitationService;
import com.sqljudge.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/role-invitations")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminRoleInvitationController {

    private final RoleInvitationService roleInvitationService;

    @Operation(
            summary = "Mời nhiều email nhận vai trò Người ra đề hoặc Kiểm duyệt",
            description =
                    "Mỗi email nhận một lời mời và link riêng, hiệu lực 7 ngày. Email đã có vai trò"
                            + " đó, là quản trị viên, bị khoá, đã xoá hoặc đang có lời mời chờ nhận"
                            + " được bỏ qua kèm lý do.")
    @ApiResponse(responseCode = "200", description = "Số lời mời đã gửi và người bị bỏ qua")
    @ApiResponse(responseCode = "400", description = "Danh sách email, vai trò hoặc lời nhắn sai")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(responseCode = "403", description = "Không phải ADMIN")
    @PostMapping
    public InviteRolesResponse invite(@Valid @RequestBody InviteRolesRequest request) {
        return roleInvitationService.invite(request, SecurityUtils.requireCurrentUser());
    }
}
