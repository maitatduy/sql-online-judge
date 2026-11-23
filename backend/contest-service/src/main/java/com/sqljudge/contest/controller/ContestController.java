package com.sqljudge.contest.controller;

import com.sqljudge.common.web.PageResponse;
import com.sqljudge.contest.dto.response.ContestSummaryResponse;
import com.sqljudge.contest.dto.response.PublicContestResponse;
import com.sqljudge.contest.dto.response.RatingChangeResponse;
import com.sqljudge.contest.entity.ContestPhase;
import com.sqljudge.contest.service.ContestQueryService;
import com.sqljudge.contest.service.ContestRatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contests")
@RequiredArgsConstructor
public class ContestController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ContestQueryService contestQueryService;
    private final ContestRatingService contestRatingService;

    @GetMapping
    @Operation(summary = "Contest theo giai đoạn tính từ giờ server")
    @ApiResponse(responseCode = "200", description = "Trang contest")
    public PageResponse<ContestSummaryResponse> list(
            @RequestParam(defaultValue = "UPCOMING") ContestPhase phase,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "Phải từ 1 trở lên") int page,
            @RequestParam(defaultValue = "20")
                    @Min(value = 1, message = "Phải từ 1 đến 100") @Max(value = MAX_PAGE_SIZE, message = "Phải từ 1 đến 100") int size) {
        return contestQueryService.list(phase, page, size);
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Chi tiết contest theo slug")
    @ApiResponse(responseCode = "200", description = "Contest")
    @ApiResponse(responseCode = "404", description = "Không có contest")
    public PublicContestResponse get(@PathVariable String slug) {
        return contestQueryService.getBySlug(slug);
    }

    @GetMapping("/{slug}/rating-changes")
    @Operation(summary = "Thay đổi rating của từng người sau contest, theo hạng")
    @ApiResponse(responseCode = "200", description = "Danh sách thay đổi rating, rỗng nếu chưa áp")
    @ApiResponse(responseCode = "404", description = "Không có contest")
    public List<RatingChangeResponse> ratingChanges(@PathVariable String slug) {
        return contestRatingService.changes(slug);
    }
}
