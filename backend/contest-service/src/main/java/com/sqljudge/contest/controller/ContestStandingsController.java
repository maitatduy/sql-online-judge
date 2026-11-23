package com.sqljudge.contest.controller;

import com.sqljudge.contest.dto.response.StandingsResponse;
import com.sqljudge.contest.dto.response.StandingsResponse.MyStanding;
import com.sqljudge.contest.service.StandingsService;
import com.sqljudge.contest.service.StandingsStreamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/contests/{slug}/standings")
@RequiredArgsConstructor
public class ContestStandingsController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Duration STREAM_TIMEOUT = Duration.ofMinutes(30);

    private final StandingsService standingsService;
    private final StandingsStreamService standingsStreamService;

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

    @GetMapping(path = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Luồng SSE báo bảng xếp hạng vừa đổi, gộp theo nhịp 2 giây")
    @ApiResponse(responseCode = "200", description = "Luồng sự kiện standings")
    @ApiResponse(responseCode = "404", description = "Không có contest")
    public SseEmitter events(@PathVariable String slug) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT.toMillis());
        standingsStreamService.register(standingsService.contestIdOf(slug), emitter);
        return emitter;
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
