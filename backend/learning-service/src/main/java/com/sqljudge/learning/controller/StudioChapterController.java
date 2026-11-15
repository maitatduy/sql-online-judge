package com.sqljudge.learning.controller;

import com.sqljudge.learning.dto.request.ChapterRequest;
import com.sqljudge.learning.dto.response.StudioChapterResponse;
import com.sqljudge.learning.service.StudioChapterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAnyRole('SETTER', 'ADMIN')")
@RequiredArgsConstructor
public class StudioChapterController {

    private final StudioChapterService studioChapterService;

    @GetMapping("/api/studio/courses/{courseId}/chapters")
    @Operation(summary = "Các chương của bản đang soạn, kèm bài giảng trong từng chương")
    @ApiResponse(responseCode = "200", description = "Chương theo thứ tự")
    @ApiResponse(responseCode = "404", description = "Không có khoá hoặc không phải của người gọi")
    public List<StudioChapterResponse> list(@PathVariable UUID courseId) {
        return studioChapterService.list(courseId);
    }

    @PostMapping("/api/studio/courses/{courseId}/chapters")
    @Operation(summary = "Thêm chương vào cuối bản nháp")
    @ApiResponse(responseCode = "201", description = "Chương vừa thêm")
    @ApiResponse(responseCode = "422", description = "Khoá đang chờ duyệt")
    public ResponseEntity<StudioChapterResponse> add(
            @PathVariable UUID courseId, @Valid @RequestBody ChapterRequest request) {
        StudioChapterResponse chapter = studioChapterService.add(courseId, request);
        return ResponseEntity.created(URI.create("/api/studio/chapters/" + chapter.id()))
                .body(chapter);
    }

    @PatchMapping("/api/studio/chapters/{chapterId}")
    @Operation(summary = "Đổi tên chương")
    @ApiResponse(responseCode = "200", description = "Chương sau khi đổi tên")
    @ApiResponse(responseCode = "422", description = "Khoá đang chờ duyệt")
    public StudioChapterResponse rename(
            @PathVariable UUID chapterId, @Valid @RequestBody ChapterRequest request) {
        return studioChapterService.rename(chapterId, request);
    }

    @DeleteMapping("/api/studio/chapters/{chapterId}")
    @Operation(summary = "Xoá chương không còn bài giảng")
    @ApiResponse(responseCode = "204", description = "Đã xoá")
    @ApiResponse(
            responseCode = "422",
            description = "Chương còn bài giảng hoặc khoá đang chờ duyệt")
    public ResponseEntity<Void> delete(@PathVariable UUID chapterId) {
        studioChapterService.delete(chapterId);
        return ResponseEntity.noContent().build();
    }
}
