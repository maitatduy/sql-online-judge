package com.sqljudge.problem.controller;

import com.sqljudge.problem.dto.response.SolvedSummaryResponse;
import com.sqljudge.problem.service.SolvedSummaryService;
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
@RequestMapping("/api/solved-summaries")
@RequiredArgsConstructor
public class SolvedSummaryController {

    private final SolvedSummaryService solvedSummaryService;

    @GetMapping("/{userId}")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Số bài đã giải của một người dùng theo độ khó, trên tổng bài công khai")
    @ApiResponse(responseCode = "200", description = "Số bài đã giải")
    public SolvedSummaryResponse summary(@PathVariable UUID userId) {
        return solvedSummaryService.summary(userId);
    }
}
