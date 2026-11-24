package com.sqljudge.ai.controller;

import com.sqljudge.ai.dto.request.SendMessageRequest;
import com.sqljudge.ai.dto.request.StartConversationRequest;
import com.sqljudge.ai.dto.response.ConversationDetailResponse;
import com.sqljudge.ai.dto.response.ConversationSummaryResponse;
import com.sqljudge.ai.dto.response.MessageExchangeResponse;
import com.sqljudge.ai.dto.response.QuotaResponse;
import com.sqljudge.ai.service.AiQuotaService;
import com.sqljudge.ai.service.AssistantChat;
import com.sqljudge.ai.service.ConversationStore;
import com.sqljudge.common.security.SecurityUtils;
import com.sqljudge.common.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class AiController {

    private static final int MAX_PAGE_SIZE = 50;

    private final AssistantChat assistantChat;
    private final ConversationStore conversationStore;
    private final AiQuotaService quotaService;

    @GetMapping("/quota")
    @Operation(summary = "Số lượt hỏi AI đã dùng hôm nay và thời điểm đặt lại")
    @ApiResponse(responseCode = "200", description = "Hạn mức hôm nay")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    public QuotaResponse quota() {
        return quotaService.usage(SecurityUtils.requireCurrentUser().id());
    }

    @GetMapping("/conversations")
    @Operation(summary = "Hội thoại của tôi, mới nhất trước")
    @ApiResponse(responseCode = "200", description = "Một trang hội thoại")
    public PageResponse<ConversationSummaryResponse> conversations(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "Phải từ 1 trở lên") int page,
            @RequestParam(defaultValue = "20")
                    @Min(value = 1, message = "Phải từ 1 đến 50") @Max(value = MAX_PAGE_SIZE, message = "Phải từ 1 đến 50") int size) {
        return conversationStore.list(SecurityUtils.requireCurrentUser().id(), page, size);
    }

    @PostMapping("/conversations")
    @Operation(summary = "Mở hội thoại mới, có thể gắn một bài tập")
    @ApiResponse(responseCode = "201", description = "Đã tạo hội thoại")
    @ApiResponse(responseCode = "403", description = "Đang thi contest tính rating")
    @ApiResponse(responseCode = "404", description = "Không có bài tập")
    public ResponseEntity<ConversationSummaryResponse> start(
            @RequestBody StartConversationRequest request) {
        ConversationSummaryResponse conversation = assistantChat.start(request.problemId());
        return ResponseEntity.created(URI.create("/api/ai/conversations/" + conversation.id()))
                .body(conversation);
    }

    @GetMapping("/conversations/{conversationId}")
    @Operation(summary = "Tin nhắn của một hội thoại của tôi")
    @ApiResponse(responseCode = "200", description = "Hội thoại")
    @ApiResponse(responseCode = "404", description = "Không có hội thoại hoặc không phải của tôi")
    public ConversationDetailResponse detail(@PathVariable UUID conversationId) {
        return conversationStore.detail(conversationId, SecurityUtils.requireCurrentUser().id());
    }

    @PostMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "Hỏi trợ lý AI, nhận câu trả lời và hạn mức còn lại")
    @ApiResponse(responseCode = "200", description = "Câu hỏi và câu trả lời đã lưu")
    @ApiResponse(responseCode = "400", description = "Câu hỏi trống hoặc quá dài")
    @ApiResponse(responseCode = "403", description = "Đang thi contest tính rating")
    @ApiResponse(responseCode = "404", description = "Không có hội thoại")
    @ApiResponse(responseCode = "429", description = "Hết lượt hỏi hôm nay")
    @ApiResponse(responseCode = "503", description = "Trợ lý AI đang bận")
    public MessageExchangeResponse send(
            @PathVariable UUID conversationId, @Valid @RequestBody SendMessageRequest request) {
        return assistantChat.send(conversationId, request);
    }
}
