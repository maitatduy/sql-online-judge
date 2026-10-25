package com.sqljudge.auth.controller;

import com.sqljudge.auth.dto.request.InviteRolesRequest;
import com.sqljudge.auth.dto.response.InviteRolesResponse;
import com.sqljudge.auth.dto.response.RoleInvitationSummaryResponse;
import com.sqljudge.auth.entity.InvitationStatus;
import com.sqljudge.auth.service.RoleInvitationAdminService;
import com.sqljudge.auth.service.RoleInvitationSearch;
import com.sqljudge.auth.service.RoleInvitationService;
import com.sqljudge.common.security.SecurityUtils;
import com.sqljudge.common.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.UUID;
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

@RestController
@RequestMapping("/api/admin/role-invitations")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminRoleInvitationController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_KEYWORD_LENGTH = 254;

    private final RoleInvitationService roleInvitationService;
    private final RoleInvitationAdminService roleInvitationAdminService;

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

    @Operation(summary = "Danh sách lời mời: lọc theo trạng thái và email, mới nhất trước")
    @ApiResponse(responseCode = "200", description = "Một trang lời mời")
    @ApiResponse(responseCode = "400", description = "Trạng thái hoặc phân trang không hợp lệ")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(responseCode = "403", description = "Không phải ADMIN")
    @GetMapping
    public PageResponse<RoleInvitationSummaryResponse> list(
            @RequestParam(required = false) InvitationStatus status,
            @RequestParam(required = false)
                    @Size(max = MAX_KEYWORD_LENGTH, message = "Từ khoá quá dài") String q,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "Phải từ 1 trở lên") int page,
            @RequestParam(defaultValue = "20")
                    @Min(value = 1, message = "Phải từ 1 đến 100") @Max(value = MAX_PAGE_SIZE, message = "Phải từ 1 đến 100") int size) {
        return roleInvitationAdminService.search(new RoleInvitationSearch(status, q), page, size);
    }

    @Operation(summary = "Huỷ một lời mời đang chờ; link trong email không dùng được nữa")
    @ApiResponse(responseCode = "204", description = "Đã huỷ")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(responseCode = "403", description = "Không phải ADMIN")
    @ApiResponse(responseCode = "404", description = "Không có lời mời")
    @ApiResponse(responseCode = "409", description = "Lời mời không còn ở trạng thái chờ")
    @PostMapping("/{id}/revoke")
    public ResponseEntity<Void> revoke(@PathVariable UUID id) {
        roleInvitationAdminService.revoke(id, SecurityUtils.requireCurrentUser());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Gửi lại lời mời đang chờ hoặc đã hết hạn với link mới, hạn 7 ngày")
    @ApiResponse(responseCode = "200", description = "Lời mời sau khi gia hạn")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(responseCode = "403", description = "Không phải ADMIN")
    @ApiResponse(responseCode = "404", description = "Không có lời mời")
    @ApiResponse(responseCode = "409", description = "Lời mời đã được nhận hoặc đã bị huỷ")
    @PostMapping("/{id}/resend")
    public RoleInvitationSummaryResponse resend(@PathVariable UUID id) {
        return roleInvitationAdminService.resend(id, SecurityUtils.requireCurrentUser());
    }
}
