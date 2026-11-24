package com.sqljudge.ai.controller;

import com.sqljudge.ai.dto.response.AiFlagResponse;
import com.sqljudge.ai.dto.response.AiSummaryResponse;
import com.sqljudge.ai.dto.response.ConversationDetailResponse;
import com.sqljudge.ai.service.AdminAiService;
import com.sqljudge.common.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/ai")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminAiController {

    private static final int MAX_PAGE_SIZE = 50;

    private final AdminAiService adminAiService;

    @GetMapping("/summary")
    @Operation(summary = "Số câu hỏi, câu trả lời bị chặn và người dùng AI hôm nay")
    @ApiResponse(responseCode = "200", description = "Số liệu hôm nay")
    @ApiResponse(responseCode = "403", description = "Không phải admin")
    public AiSummaryResponse summary() {
        return adminAiService.summary();
    }

    @GetMapping("/flags")
    @Operation(summary = "Câu trả lời bị chặn, mới nhất trước, kèm câu hỏi đã dẫn tới")
    @ApiResponse(responseCode = "200", description = "Một trang câu trả lời bị chặn")
    @ApiResponse(responseCode = "403", description = "Không phải admin")
    public PageResponse<AiFlagResponse> flags(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "Phải từ 1 trở lên") int page,
            @RequestParam(defaultValue = "20")
                    @Min(value = 1, message = "Phải từ 1 đến 50") @Max(value = MAX_PAGE_SIZE, message = "Phải từ 1 đến 50") int size) {
        return adminAiService.flags(page, size);
    }

    @GetMapping("/conversations/{conversationId}")
    @Operation(summary = "Toàn bộ tin nhắn của một hội thoại để kiểm tra")
    @ApiResponse(responseCode = "200", description = "Hội thoại")
    @ApiResponse(responseCode = "404", description = "Không có hội thoại")
    public ConversationDetailResponse conversation(@PathVariable UUID conversationId) {
        return adminAiService.conversation(conversationId);
    }
}
