package com.sqljudge.contest.controller;

import com.sqljudge.common.web.PageResponse;
import com.sqljudge.contest.dto.response.LeaderboardEntryResponse;
import com.sqljudge.contest.service.LeaderboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private static final int MAX_PAGE_SIZE = 100;

    private final LeaderboardService leaderboardService;

    @GetMapping
    @PreAuthorize("permitAll()")
    @Operation(summary = "Bảng xếp hạng rating của người đã thi ít nhất một contest tính rating")
    @ApiResponse(responseCode = "200", description = "Một trang xếp hạng, rating cao trước")
    @ApiResponse(responseCode = "400", description = "Tham số phân trang sai")
    public PageResponse<LeaderboardEntryResponse> leaderboard(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "Phải từ 1 trở lên") int page,
            @RequestParam(defaultValue = "50")
                    @Min(value = 1, message = "Phải từ 1 đến 100") @Max(value = MAX_PAGE_SIZE, message = "Phải từ 1 đến 100") int size) {
        return leaderboardService.leaderboard(page, size);
    }
}
