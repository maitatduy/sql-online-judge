package com.sqljudge.learning.controller;

import com.sqljudge.learning.dto.request.CreateCourseRequest;
import com.sqljudge.learning.dto.request.UpdateCourseRequest;
import com.sqljudge.learning.dto.response.StudioCourseResponse;
import com.sqljudge.learning.service.StudioCourseDraftService;
import com.sqljudge.learning.service.StudioCourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/studio/courses")
@PreAuthorize("hasAnyRole('SETTER', 'ADMIN')")
@RequiredArgsConstructor
public class StudioCourseController {

    private final StudioCourseService studioCourseService;
    private final StudioCourseDraftService studioCourseDraftService;

    @PostMapping("/{courseId}/draft")
    @Operation(summary = "Tạo bản nháp mới từ bản đang xuất bản để sửa khoá")
    @ApiResponse(responseCode = "201", description = "Bản nháp vừa tạo, đã chép đủ nội dung")
    @ApiResponse(responseCode = "409", description = "Khoá đã có bản nháp đang soạn")
    @ApiResponse(responseCode = "422", description = "Khoá chưa xuất bản")
    public ResponseEntity<StudioCourseResponse> createDraft(@PathVariable UUID courseId) {
        StudioCourseResponse draft = studioCourseDraftService.createDraft(courseId);
        return ResponseEntity.created(URI.create("/api/studio/courses/" + courseId)).body(draft);
    }

    @PostMapping
    @Operation(summary = "Tạo khoá học nháp của người gọi")
    @ApiResponse(responseCode = "201", description = "Khoá nháp vừa tạo")
    @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    @ApiResponse(responseCode = "409", description = "Slug đã có khoá khác dùng")
    public ResponseEntity<StudioCourseResponse> create(
            @Valid @RequestBody CreateCourseRequest request) {
        StudioCourseResponse course = studioCourseService.create(request);
        return ResponseEntity.created(URI.create("/api/studio/courses/" + course.id()))
                .body(course);
    }

    @GetMapping
    @Operation(summary = "Các khoá học người gọi đang soạn, sửa gần nhất trước")
    @ApiResponse(responseCode = "200", description = "Khoá học của người gọi")
    public List<StudioCourseResponse> listMine() {
        return studioCourseService.listMine();
    }

    @GetMapping("/{courseId}")
    @Operation(summary = "Bản đang soạn của một khoá học, chỉ chủ khoá hoặc ADMIN")
    @ApiResponse(responseCode = "200", description = "Khoá học")
    @ApiResponse(responseCode = "404", description = "Không có khoá hoặc không phải của người gọi")
    public StudioCourseResponse get(@PathVariable UUID courseId) {
        return studioCourseService.get(courseId);
    }

    @PatchMapping("/{courseId}")
    @Operation(summary = "Sửa tên, tóm tắt và độ khó của bản nháp")
    @ApiResponse(responseCode = "200", description = "Khoá học sau khi sửa")
    @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    @ApiResponse(responseCode = "422", description = "Khoá đang chờ duyệt")
    public StudioCourseResponse update(
            @PathVariable UUID courseId, @Valid @RequestBody UpdateCourseRequest request) {
        return studioCourseService.update(courseId, request);
    }

    @PostMapping("/{courseId}/submit")
    @Operation(summary = "Gửi bản nháp cho ADMIN duyệt")
    @ApiResponse(responseCode = "200", description = "Khoá chuyển sang chờ duyệt")
    @ApiResponse(
            responseCode = "422",
            description = "Khoá chưa có bài giảng hoặc không còn là bản nháp")
    public StudioCourseResponse submit(@PathVariable UUID courseId) {
        return studioCourseService.submit(courseId);
    }
}
