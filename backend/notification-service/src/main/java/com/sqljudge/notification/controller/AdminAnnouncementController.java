package com.sqljudge.notification.controller;

import com.sqljudge.common.web.PageResponse;
import com.sqljudge.notification.dto.request.SendAnnouncementRequest;
import com.sqljudge.notification.dto.response.AnnouncementResponse;
import com.sqljudge.notification.service.AnnouncementDelivery;
import com.sqljudge.notification.service.AnnouncementStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/announcements")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminAnnouncementController {

    private static final int MAX_PAGE_SIZE = 50;

    private final AnnouncementStore announcementStore;
    private final AnnouncementDelivery announcementDelivery;

    @PostMapping
    @Operation(summary = "Gửi thông báo hệ thống tới mọi người dùng, gửi nền")
    @ApiResponse(responseCode = "202", description = "Đã nhận, đang gửi")
    @ApiResponse(responseCode = "400", description = "Tiêu đề trống hoặc đường dẫn sai")
    @ApiResponse(responseCode = "403", description = "Không phải admin")
    public ResponseEntity<AnnouncementResponse> send(
            @Valid @RequestBody SendAnnouncementRequest request) {
        AnnouncementResponse announcement =
                announcementStore.create(request.title(), request.link());
        announcementDelivery.deliver(announcement);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(announcement);
    }

    @GetMapping
    @Operation(summary = "Thông báo hệ thống đã gửi, mới nhất trước")
    @ApiResponse(responseCode = "200", description = "Một trang thông báo hệ thống")
    public PageResponse<AnnouncementResponse> list(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "Phải từ 1 trở lên") int page,
            @RequestParam(defaultValue = "20")
                    @Min(value = 1, message = "Phải từ 1 đến 50") @Max(value = MAX_PAGE_SIZE, message = "Phải từ 1 đến 50") int size) {
        return announcementStore.list(page, size);
    }
}
