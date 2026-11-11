package com.sqljudge.problem.controller;

import com.sqljudge.problem.dto.response.InternalJudgePackageResponse;
import com.sqljudge.problem.service.InternalJudgePackageService;
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
@RequestMapping("/internal/problem-revisions")
@PreAuthorize("hasRole('INTERNAL_SERVICE')")
@RequiredArgsConstructor
public class InternalProblemRevisionController {

    private final InternalJudgePackageService internalJudgePackageService;

    @GetMapping("/{revisionId}/judge-package")
    @Operation(summary = "Gói chấm của bản đã chốt: cài đặt so khớp, testcase và kết quả mong đợi")
    @ApiResponse(responseCode = "200", description = "Gói chấm, gồm cả testcase ẩn")
    @ApiResponse(responseCode = "401", description = "Thiếu hoặc sai token nội bộ")
    @ApiResponse(responseCode = "404", description = "Không có bản này hoặc bản chưa chốt")
    public InternalJudgePackageResponse judgePackage(@PathVariable UUID revisionId) {
        return internalJudgePackageService.judgePackage(revisionId);
    }
}
