package com.sqljudge.contest.controller;

import com.sqljudge.contest.dto.response.StandingsResponse;
import com.sqljudge.contest.dto.response.StandingsResponse.MyStanding;
import com.sqljudge.contest.service.StandingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contests/{slug}/standings")
@RequiredArgsConstructor
public class ContestStandingsController {

    private static final int MAX_PAGE_SIZE = 100;

    private final StandingsService standingsService;

    @GetMapping
    @Operation(summary = "Bảng xếp hạng công khai, đóng băng ở các phút cuối")
    @ApiResponse(responseCode = "200", description = "Một trang bảng xếp hạng")
    @ApiResponse(responseCode = "404", description = "Không có contest")
    public StandingsResponse standings(
            @PathVariable String slug,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "Phải từ 1 trở lên") int page,
            @RequestParam(defaultValue = "50")
                    @Min(value = 1, message = "Phải từ 1 đến 100") @Max(value = MAX_PAGE_SIZE, message = "Phải từ 1 đến 100") int size) {
        return standingsService.standings(slug, page, size);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Kết quả thật của người gọi, kể cả khi bảng đang đóng băng")
    @ApiResponse(responseCode = "200", description = "Kết quả của người gọi")
    @ApiResponse(responseCode = "404", description = "Không có contest hoặc chưa đăng ký")
    public MyStanding mine(@PathVariable String slug) {
        return standingsService.mine(slug);
    }
}
