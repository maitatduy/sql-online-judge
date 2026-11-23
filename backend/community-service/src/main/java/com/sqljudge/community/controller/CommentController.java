package com.sqljudge.community.controller;

import com.sqljudge.common.web.PageResponse;
import com.sqljudge.community.dto.request.EditCommentRequest;
import com.sqljudge.community.dto.request.VoteRequest;
import com.sqljudge.community.dto.request.WriteCommentRequest;
import com.sqljudge.community.dto.response.CommentResponse;
import com.sqljudge.community.dto.response.ContributorResponse;
import com.sqljudge.community.dto.response.VoteResponse;
import com.sqljudge.community.service.BookmarkService;
import com.sqljudge.community.service.CommentQueryService;
import com.sqljudge.community.service.CommentSearch;
import com.sqljudge.community.service.CommentSort;
import com.sqljudge.community.service.CommentWriter;
import com.sqljudge.community.service.ContributorService;
import com.sqljudge.community.service.VoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommentController {

    private static final int MAX_PAGE_SIZE = 50;

    private final CommentQueryService commentQueryService;
    private final CommentWriter commentWriter;
    private final VoteService voteService;
    private final BookmarkService bookmarkService;
    private final ContributorService contributorService;

    @GetMapping("/comments")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Bình luận gốc của một bài viết hoặc một bài tập, kèm trả lời")
    @ApiResponse(responseCode = "200", description = "Một trang bình luận")
    public PageResponse<CommentResponse> list(
            @RequestParam(required = false) UUID postId,
            @RequestParam(required = false) UUID problemId,
            @RequestParam(defaultValue = "TOP") CommentSort sort,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "Phải từ 1 trở lên") int page,
            @RequestParam(defaultValue = "20")
                    @Min(value = 1, message = "Phải từ 1 đến 50") @Max(value = MAX_PAGE_SIZE, message = "Phải từ 1 đến 50") int size) {
        return commentQueryService.list(new CommentSearch(postId, problemId, sort), page, size);
    }

    @PostMapping("/comments")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Bình luận vào bài viết hoặc bài tập, hoặc trả lời một bình luận gốc")
    @ApiResponse(responseCode = "201", description = "Đã bình luận")
    @ApiResponse(responseCode = "404", description = "Không có bài viết hoặc bình luận gốc")
    @ApiResponse(responseCode = "422", description = "Sai chủ đề bình luận")
    public ResponseEntity<Void> write(@Valid @RequestBody WriteCommentRequest request) {
        UUID commentId = commentWriter.write(request);
        return ResponseEntity.created(URI.create("/api/community/comments/" + commentId)).build();
    }

    @PutMapping("/comments/{commentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Sửa bình luận của chính mình")
    @ApiResponse(responseCode = "204", description = "Đã sửa")
    @ApiResponse(responseCode = "403", description = "Không phải tác giả")
    public ResponseEntity<Void> edit(
            @PathVariable UUID commentId, @Valid @RequestBody EditCommentRequest request) {
        commentWriter.edit(commentId, request.body());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/comments/{commentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Xoá bình luận: tác giả, kiểm duyệt viên hoặc admin")
    @ApiResponse(responseCode = "204", description = "Đã xoá")
    @ApiResponse(responseCode = "403", description = "Không có quyền")
    public ResponseEntity<Void> delete(@PathVariable UUID commentId) {
        commentWriter.delete(commentId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/votes")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Vote lên, xuống hoặc bỏ vote cho bài viết hay bình luận")
    @ApiResponse(responseCode = "200", description = "Điểm mới và vote của bạn")
    @ApiResponse(responseCode = "404", description = "Không có đối tượng")
    public VoteResponse vote(@Valid @RequestBody VoteRequest request) {
        return voteService.vote(request);
    }

    @PutMapping("/posts/{postId}/bookmark")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lưu bài viết")
    @ApiResponse(responseCode = "204", description = "Đã lưu")
    public ResponseEntity<Void> bookmark(@PathVariable UUID postId) {
        bookmarkService.save(postId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/posts/{postId}/bookmark")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Bỏ lưu bài viết")
    @ApiResponse(responseCode = "204", description = "Đã bỏ lưu")
    public ResponseEntity<Void> unbookmark(@PathVariable UUID postId) {
        bookmarkService.remove(postId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/contributors")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Năm người nhận nhiều điểm vote nhất trong 7 ngày")
    @ApiResponse(responseCode = "200", description = "Người đóng góp nhiều tuần này")
    public List<ContributorResponse> contributors() {
        return contributorService.topThisWeek();
    }
}
