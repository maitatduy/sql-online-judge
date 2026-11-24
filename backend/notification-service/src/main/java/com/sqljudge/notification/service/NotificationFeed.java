package com.sqljudge.notification.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.notification.dto.response.NotificationFeedResponse;
import com.sqljudge.notification.dto.response.NotificationResponse;
import com.sqljudge.notification.dto.response.UnreadCountResponse;
import com.sqljudge.notification.entity.Notification;
import com.sqljudge.notification.repository.NotificationRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationFeed {

    private final NotificationRepository notificationRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public NotificationFeedResponse feed(UUID userId, UUID cursor, int size) {
        PageRequest page = PageRequest.of(0, size);
        List<Notification> items =
                cursor == null
                        ? notificationRepository.findByUserIdOrderByIdDesc(userId, page)
                        : notificationRepository.findByUserIdAndIdLessThanOrderByIdDesc(
                                userId, cursor, page);
        String nextCursor =
                items.size() < size ? null : items.get(items.size() - 1).getId().toString();
        return new NotificationFeedResponse(
                items.stream().map(NotificationResponse::from).toList(), nextCursor);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount(UUID userId) {
        return new UnreadCountResponse(notificationRepository.countByUserIdAndReadAtIsNull(userId));
    }

    @Transactional
    public void markRead(UUID userId, UUID notificationId) {
        notificationRepository
                .findByIdAndUserId(notificationId, userId)
                .orElseThrow(
                        () ->
                                new NotFoundException(
                                        ErrorCode.NOTIFICATION_NOT_FOUND,
                                        "Không tìm thấy thông báo"))
                .markRead(clock.instant());
    }

    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepository.markAllRead(userId, clock.instant());
    }
}
