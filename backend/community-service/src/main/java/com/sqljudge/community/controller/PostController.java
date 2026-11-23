package com.sqljudge.community.controller;

import com.sqljudge.common.web.PageResponse;
import com.sqljudge.community.dto.request.SavePostRequest;
import com.sqljudge.community.dto.response.CommunityOverviewResponse;
import com.sqljudge.community.dto.response.PostDetailResponse;
import com.sqljudge.community.dto.response.PostSummaryResponse;
import com.sqljudge.community.entity.PostCategory;
import com.sqljudge.community.service.PostCommandService;
import com.sqljudge.community.service.PostQueryService;
import com.sqljudge.community.service.PostSearch;
import com.sqljudge.community.service.PostTab;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.net.URI;
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
public class PostController {

    private static final int MAX_PAGE_SIZE = 50;

    private final PostQueryService postQueryService;
    private final PostCommandService postCommandService;

    @GetMapping("/posts")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Bài viết đã đăng theo chuyên mục, tab, tag, bài tập hoặc từ khoá")
    @ApiResponse(responseCode = "200", description = "Một trang bài viết")
    @ApiResponse(responseCode = "400", description = "Tham số lọc hoặc phân trang sai")
    public PageResponse<PostSummaryResponse> list(
            @RequestParam(required = false) PostCategory category,
            @RequestParam(defaultValue = "HOT") PostTab tab,
            @RequestParam(required = false) @Size(max = 100, message = "Tối đa 100 ký tự") String q,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) UUID problemId,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "Phải từ 1 trở lên") int page,
            @RequestParam(defaultValue = "20")
                    @Min(value = 1, message = "Phải từ 1 đến 50") @Max(value = MAX_PAGE_SIZE, message = "Phải từ 1 đến 50") int size) {
        return postQueryService.list(new PostSearch(category, tab, q, tag, problemId), page, size);
    }

    @GetMapping("/posts/{postId}")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Chi tiết một bài viết; bản nháp chỉ tác giả xem được")
    @ApiResponse(responseCode = "200", description = "Bài viết")
    @ApiResponse(responseCode = "404", description = "Không có bài viết hoặc không được xem")
    public PostDetailResponse detail(@PathVariable UUID postId) {
        return postQueryService.detail(postId);
    }

    @GetMapping("/overview")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Số bài viết đã đăng theo chuyên mục")
    @ApiResponse(responseCode = "200", description = "Số liệu cộng đồng")
    public CommunityOverviewResponse overview() {
        return postQueryService.overview();
    }

    @PostMapping("/posts")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Viết bài: lưu nháp hoặc đăng ngay")
    @ApiResponse(responseCode = "201", description = "Đã tạo bài viết")
    @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    public ResponseEntity<PostDetailResponse> create(@Valid @RequestBody SavePostRequest request) {
        UUID postId = postCommandService.create(request);
        return ResponseEntity.created(URI.create("/api/community/posts/" + postId))
                .body(postQueryService.detail(postId));
    }

    @PutMapping("/posts/{postId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Sửa bài viết của chính mình, có thể đăng bản nháp")
    @ApiResponse(responseCode = "200", description = "Đã sửa")
    @ApiResponse(responseCode = "403", description = "Không phải tác giả")
    @ApiResponse(responseCode = "404", description = "Không có bài viết")
    public PostDetailResponse update(
            @PathVariable UUID postId, @Valid @RequestBody SavePostRequest request) {
        postCommandService.update(postId, request);
        return postQueryService.detail(postId);
    }

    @DeleteMapping("/posts/{postId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Xoá mềm bài viết: tác giả, kiểm duyệt viên hoặc admin")
    @ApiResponse(responseCode = "204", description = "Đã xoá")
    @ApiResponse(responseCode = "403", description = "Không có quyền")
    @ApiResponse(responseCode = "404", description = "Không có bài viết")
    public ResponseEntity<Void> delete(@PathVariable UUID postId) {
        postCommandService.delete(postId);
        return ResponseEntity.noContent().build();
    }
}
