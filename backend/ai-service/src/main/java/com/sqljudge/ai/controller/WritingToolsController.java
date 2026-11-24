package com.sqljudge.ai.controller;

import com.sqljudge.ai.dto.request.DiscussionSummaryRequest;
import com.sqljudge.ai.dto.request.ProofreadRequest;
import com.sqljudge.ai.dto.response.AiTextResponse;
import com.sqljudge.ai.service.WritingTools;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class WritingToolsController {

    private final WritingTools writingTools;

    @PostMapping("/discussion-summaries")
    @Operation(summary = "Tóm tắt bài viết cộng đồng và các bình luận của nó")
    @ApiResponse(responseCode = "200", description = "Bản tóm tắt và hạn mức còn lại")
    @ApiResponse(responseCode = "403", description = "Đang thi contest tính rating")
    @ApiResponse(responseCode = "404", description = "Không có bài viết")
    @ApiResponse(responseCode = "429", description = "Hết lượt hỏi hôm nay")
    public AiTextResponse summarize(@Valid @RequestBody DiscussionSummaryRequest request) {
        return writingTools.summarizeDiscussion(request.postId());
    }

    @PostMapping("/proofreads")
    @Operation(summary = "Sửa chính tả bài viết Markdown, giữ nguyên khối code")
    @ApiResponse(responseCode = "200", description = "Nội dung đã sửa và hạn mức còn lại")
    @ApiResponse(responseCode = "400", description = "Nội dung trống hoặc quá dài")
    @ApiResponse(responseCode = "429", description = "Hết lượt hỏi hôm nay")
    public AiTextResponse proofread(@Valid @RequestBody ProofreadRequest request) {
        return writingTools.proofread(request.text());
    }
}
