package com.sqljudge.problem.controller;

import com.sqljudge.problem.dto.response.AdminTagResponse;
import com.sqljudge.problem.service.AdminTagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/tags")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminTagController {

    private final AdminTagService adminTagService;

    @GetMapping
    @Operation(summary = "Mọi tag kèm số bài chưa xoá đang dùng")
    @ApiResponse(responseCode = "200", description = "Danh sách tag")
    @ApiResponse(responseCode = "403", description = "Không phải ADMIN")
    public List<AdminTagResponse> list() {
        return adminTagService.list();
    }
}
