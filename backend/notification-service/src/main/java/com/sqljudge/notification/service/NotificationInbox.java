package com.sqljudge.notification.service;

import com.sqljudge.notification.entity.Notification;
import com.sqljudge.notification.entity.Notification.NewNotification;
import com.sqljudge.notification.repository.NotificationRepository;
import com.sqljudge.notification.service.NotificationsDelivered.Delivered;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationInbox {

    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public int deliver(List<NewNotification> drafts) {
        List<Delivered> delivered =
                drafts.stream()
                        .filter(
                                draft ->
                                        !notificationRepository.existsByEventIdAndUserId(
                                                draft.eventId(), draft.userId()))
                        .map(Notification::of)
                        .map(notificationRepository::save)
                        .map(
                                saved ->
                                        new Delivered(
                                                saved.getUserId(),
                                                saved.getType(),
                                                saved.getTitle(),
                                                saved.getLink()))
                        .toList();
        if (!delivered.isEmpty()) {
            eventPublisher.publishEvent(new NotificationsDelivered(delivered));
        }
        log.info(
                "notifications_delivered count={} eventId={}",
                drafts.size(),
                drafts.isEmpty() ? null : drafts.get(0).eventId());
        return delivered.size();
    }
}
