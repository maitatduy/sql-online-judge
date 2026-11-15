package com.sqljudge.learning.controller;

import com.sqljudge.learning.dto.request.LessonRequest;
import com.sqljudge.learning.dto.response.StudioLessonResponse;
import com.sqljudge.learning.service.StudioLessonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.net.URI;
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
public class StudioLessonController {

    private final StudioLessonService studioLessonService;

    @PostMapping("/api/studio/chapters/{chapterId}/lessons")
    @Operation(summary = "Thêm bài giảng vào cuối chương")
    @ApiResponse(responseCode = "201", description = "Bài giảng vừa thêm")
    @ApiResponse(responseCode = "409", description = "Slug đã có trong khoá")
    @ApiResponse(responseCode = "422", description = "Khoá đang chờ duyệt")
    public ResponseEntity<StudioLessonResponse> add(
            @PathVariable UUID chapterId, @Valid @RequestBody LessonRequest request) {
        StudioLessonResponse lesson = studioLessonService.add(chapterId, request);
        return ResponseEntity.created(URI.create("/api/studio/lessons/" + lesson.id()))
                .body(lesson);
    }

    @GetMapping("/api/studio/lessons/{lessonId}")
    @Operation(summary = "Nội dung bài giảng đang soạn")
    @ApiResponse(responseCode = "200", description = "Bài giảng")
    @ApiResponse(responseCode = "404", description = "Không có bài hoặc không phải của người gọi")
    public StudioLessonResponse get(@PathVariable UUID lessonId) {
        return studioLessonService.get(lessonId);
    }

    @PatchMapping("/api/studio/lessons/{lessonId}")
    @Operation(summary = "Sửa slug, tên và nội dung bài giảng")
    @ApiResponse(responseCode = "200", description = "Bài giảng sau khi sửa")
    @ApiResponse(responseCode = "409", description = "Slug đã có trong khoá")
    @ApiResponse(responseCode = "422", description = "Khoá đang chờ duyệt")
    public StudioLessonResponse update(
            @PathVariable UUID lessonId, @Valid @RequestBody LessonRequest request) {
        return studioLessonService.update(lessonId, request);
    }

    @DeleteMapping("/api/studio/lessons/{lessonId}")
    @Operation(summary = "Xoá bài giảng cùng ô SQL và quiz của nó")
    @ApiResponse(responseCode = "204", description = "Đã xoá")
    @ApiResponse(responseCode = "422", description = "Khoá đang chờ duyệt")
    public ResponseEntity<Void> delete(@PathVariable UUID lessonId) {
        studioLessonService.delete(lessonId);
        return ResponseEntity.noContent().build();
    }
}
