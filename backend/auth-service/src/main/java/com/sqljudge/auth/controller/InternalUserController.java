package com.sqljudge.auth.controller;

import com.sqljudge.auth.dto.response.InternalUserResponse;
import com.sqljudge.auth.service.InternalUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/users")
@PreAuthorize("hasRole('INTERNAL_SERVICE')")
@RequiredArgsConstructor
public class InternalUserController {

    private final InternalUserService internalUserService;

    @GetMapping("/{userId}")
    @Operation(summary = "Tên và quyền thi contest tính rating của một người dùng")
    @ApiResponse(responseCode = "200", description = "Người dùng còn hoạt động")
    @ApiResponse(responseCode = "401", description = "Thiếu hoặc sai token nội bộ")
    @ApiResponse(responseCode = "404", description = "Không có người dùng hoặc đã xoá")
    public InternalUserResponse find(@PathVariable UUID userId) {
        return internalUserService.find(userId);
    }
}
