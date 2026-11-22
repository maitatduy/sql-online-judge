package com.sqljudge.contest.controller;

import com.sqljudge.contest.dto.response.ContestantProblemResponse;
import com.sqljudge.contest.service.ContestantProblemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contests/{slug}/problems")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ContestProblemController {

    private final ContestantProblemService contestantProblemService;

    @GetMapping
    @Operation(summary = "Bài của contest theo chữ cái, chỉ khi contest đã bắt đầu")
    @ApiResponse(responseCode = "200", description = "Danh sách bài")
    @ApiResponse(
            responseCode = "403",
            description = "Contest chưa bắt đầu, hoặc đang thi mà chưa đăng ký")
    @ApiResponse(responseCode = "404", description = "Không có contest")
    public List<ContestantProblemResponse> list(@PathVariable String slug) {
        return contestantProblemService.list(slug);
    }
}
