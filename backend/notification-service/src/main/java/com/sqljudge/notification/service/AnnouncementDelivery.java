package com.sqljudge.notification.service;

import com.sqljudge.notification.client.AuthServiceClient;
import com.sqljudge.notification.dto.response.AnnouncementResponse;
import com.sqljudge.notification.entity.Notification.NewNotification;
import com.sqljudge.notification.entity.NotificationType;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnnouncementDelivery {

    private static final int BATCH_SIZE = 500;

    private final AuthServiceClient authServiceClient;
    private final NotificationInbox inbox;
    private final AnnouncementStore announcementStore;

    @Async
    public void deliver(AnnouncementResponse announcement) {
        int recipients = 0;
        UUID afterId = null;
        try {
            while (true) {
                List<UUID> userIds = authServiceClient.activeIds(afterId, BATCH_SIZE);
                recipients += inbox.deliver(draftsFor(announcement, userIds));
                if (userIds.size() < BATCH_SIZE) {
                    break;
                }
                afterId = userIds.get(userIds.size() - 1);
            }
            announcementStore.markDelivered(announcement.id(), recipients);
            log.info(
                    "announcement_delivered announcementId={} recipients={}",
                    announcement.id(),
                    recipients);
        } catch (RuntimeException error) {
            log.error(
                    "announcement_delivery_failed announcementId={} delivered={}",
                    announcement.id(),
                    recipients,
                    error);
        }
    }

    private static List<NewNotification> draftsFor(
            AnnouncementResponse announcement, List<UUID> userIds) {
        return userIds.stream()
                .map(
                        userId ->
                                new NewNotification(
                                        userId,
                                        announcement.id(),
                                        NotificationType.ANNOUNCEMENT,
                                        announcement.title(),
                                        announcement.link()))
                .toList();
    }
}
