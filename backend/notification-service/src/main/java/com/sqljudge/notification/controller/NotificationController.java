package com.sqljudge.notification.controller;

import com.sqljudge.common.security.SecurityUtils;
import com.sqljudge.notification.dto.request.UpdatePreferenceRequest;
import com.sqljudge.notification.dto.response.NotificationFeedResponse;
import com.sqljudge.notification.dto.response.PreferenceResponse;
import com.sqljudge.notification.dto.response.UnreadCountResponse;
import com.sqljudge.notification.service.NotificationFeed;
import com.sqljudge.notification.service.NotificationPreferences;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class NotificationController {

    private static final int MAX_PAGE_SIZE = 50;

    private final NotificationFeed notificationFeed;
    private final NotificationPreferences notificationPreferences;

    @GetMapping
    @Operation(summary = "Thông báo của tôi, mới nhất trước, phân trang bằng cursor")
    @ApiResponse(responseCode = "200", description = "Một trang thông báo")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    public NotificationFeedResponse feed(
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "20")
                    @Min(value = 1, message = "Phải từ 1 đến 50") @Max(value = MAX_PAGE_SIZE, message = "Phải từ 1 đến 50") int size) {
        return notificationFeed.feed(currentUserId(), cursor, size);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Số thông báo chưa đọc")
    @ApiResponse(responseCode = "200", description = "Số chưa đọc")
    public UnreadCountResponse unreadCount() {
        return notificationFeed.unreadCount(currentUserId());
    }

    @PostMapping("/{notificationId}/read")
    @Operation(summary = "Đánh dấu một thông báo đã đọc")
    @ApiResponse(responseCode = "204", description = "Đã đánh dấu")
    @ApiResponse(responseCode = "404", description = "Không có thông báo hoặc không phải của tôi")
    public ResponseEntity<Void> markRead(@PathVariable UUID notificationId) {
        notificationFeed.markRead(currentUserId(), notificationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    @Operation(summary = "Đánh dấu mọi thông báo đã đọc")
    @ApiResponse(responseCode = "204", description = "Đã đánh dấu")
    public ResponseEntity<Void> markAllRead() {
        notificationFeed.markAllRead(currentUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/preferences")
    @Operation(summary = "Tuỳ chọn nhận email thông báo của tôi")
    @ApiResponse(responseCode = "200", description = "Tuỳ chọn hiện tại")
    public PreferenceResponse preferences() {
        return notificationPreferences.get(currentUserId());
    }

    @PutMapping("/preferences")
    @Operation(summary = "Bật hoặc tắt email thông báo")
    @ApiResponse(responseCode = "200", description = "Đã lưu")
    @ApiResponse(responseCode = "400", description = "Thiếu lựa chọn")
    public PreferenceResponse updatePreferences(
            @Valid @RequestBody UpdatePreferenceRequest request) {
        return notificationPreferences.update(currentUserId(), request.isEmailEnabled());
    }

    private static UUID currentUserId() {
        return SecurityUtils.requireCurrentUser().id();
    }
}
