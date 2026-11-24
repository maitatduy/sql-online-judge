package com.sqljudge.notification.service;

import com.sqljudge.notification.entity.Notification;
import com.sqljudge.notification.entity.Notification.NewNotification;
import com.sqljudge.notification.repository.NotificationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationInbox {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void deliver(List<NewNotification> drafts) {
        drafts.stream()
                .filter(
                        draft ->
                                !notificationRepository.existsByEventIdAndUserId(
                                        draft.eventId(), draft.userId()))
                .map(Notification::of)
                .forEach(notificationRepository::save);
        log.info(
                "notifications_delivered count={} eventId={}",
                drafts.size(),
                drafts.isEmpty() ? null : drafts.get(0).eventId());
    }
}
