package com.sqljudge.community.controller;

import com.sqljudge.community.dto.response.ProblemDiscussionResponse;
import com.sqljudge.community.service.ProblemDiscussionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProblemDiscussionController {

    private static final int MAX_SIZE = 20;

    private final ProblemDiscussionService problemDiscussionService;

    @GetMapping("/api/community/problem-discussions")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Bình luận mới nhất trong tab Thảo luận của các bài tập")
    @ApiResponse(responseCode = "200", description = "Mới nhất trước, bỏ bình luận đang ẩn")
    @ApiResponse(responseCode = "400", description = "Số lượng ngoài khoảng 1 đến 20")
    public List<ProblemDiscussionResponse> latest(
            @RequestParam(defaultValue = "6")
                    @Min(value = 1, message = "Phải từ 1 đến 20") @Max(value = MAX_SIZE, message = "Phải từ 1 đến 20") int size) {
        return problemDiscussionService.latest(size);
    }
}
