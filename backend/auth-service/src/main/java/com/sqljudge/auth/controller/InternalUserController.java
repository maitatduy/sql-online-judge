package com.sqljudge.auth.controller;

import com.sqljudge.auth.dto.response.InternalUserAccountResponse;
import com.sqljudge.auth.dto.response.InternalUserContactResponse;
import com.sqljudge.auth.dto.response.InternalUserNameResponse;
import com.sqljudge.auth.dto.response.InternalUserResponse;
import com.sqljudge.auth.service.InternalUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/users")
@PreAuthorize("hasRole('INTERNAL_SERVICE')")
@RequiredArgsConstructor
public class InternalUserController {

    private static final int MAX_IDS = 100;
    private static final int MAX_ID_PAGE = 1000;

    private final InternalUserService internalUserService;

    @GetMapping("/active-ids")
    @Operation(summary = "Id của người dùng chưa xoá, tăng dần, sau một id cho trước")
    @ApiResponse(responseCode = "200", description = "Một trang id")
    @ApiResponse(responseCode = "401", description = "Thiếu hoặc sai token nội bộ")
    public List<UUID> activeIds(
            @RequestParam(required = false) UUID afterId,
            @RequestParam(defaultValue = "500")
                    @Min(value = 1, message = "Phải từ 1 đến 1000") @Max(value = MAX_ID_PAGE, message = "Phải từ 1 đến 1000") int size) {
        return internalUserService.activeIds(afterId, size);
    }

    @GetMapping("/{userId}/contact")
    @Operation(summary = "Email để gửi thông báo cho một người dùng chưa xoá")
    @ApiResponse(responseCode = "200", description = "Thông tin liên hệ")
    @ApiResponse(responseCode = "401", description = "Thiếu hoặc sai token nội bộ")
    @ApiResponse(responseCode = "404", description = "Không có người dùng hoặc đã xoá")
    public InternalUserContactResponse contact(@PathVariable UUID userId) {
        return internalUserService.contact(userId);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Tên và quyền thi contest tính rating của một người dùng")
    @ApiResponse(responseCode = "200", description = "Người dùng còn hoạt động")
    @ApiResponse(responseCode = "401", description = "Thiếu hoặc sai token nội bộ")
    @ApiResponse(responseCode = "404", description = "Không có người dùng hoặc đã xoá")
    public InternalUserResponse find(@PathVariable UUID userId) {
        return internalUserService.find(userId);
    }

    @GetMapping
    @Operation(summary = "Tên của nhiều người dùng còn hoạt động, tối đa 100 người một lần")
    @ApiResponse(responseCode = "200", description = "Người dùng còn hoạt động trong danh sách")
    @ApiResponse(responseCode = "400", description = "Quá 100 id")
    @ApiResponse(responseCode = "401", description = "Thiếu hoặc sai token nội bộ")
    public List<InternalUserNameResponse> findNames(
            @RequestParam @Size(max = MAX_IDS, message = "Tối đa 100 người dùng") List<UUID> ids) {
        return internalUserService.findNames(ids);
    }

    @GetMapping("/by-username/{username}")
    @Operation(summary = "Vai trò và trạng thái hoạt động của một tài khoản theo username")
    @ApiResponse(responseCode = "200", description = "Tài khoản chưa bị xoá")
    @ApiResponse(responseCode = "401", description = "Thiếu hoặc sai token nội bộ")
    @ApiResponse(responseCode = "404", description = "Không có tài khoản hoặc đã xoá")
    public InternalUserAccountResponse findByUsername(@PathVariable String username) {
        return internalUserService.findByUsername(username);
    }
}
