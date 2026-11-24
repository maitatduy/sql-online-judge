package com.sqljudge.notification.service;

import com.sqljudge.notification.entity.Notification.NewNotification;
import com.sqljudge.notification.entity.NotificationType;
import com.sqljudge.notification.event.IncomingEvents.Commented;
import com.sqljudge.notification.event.IncomingEvents.ProblemPublished;
import com.sqljudge.notification.event.IncomingEvents.RatingUpdated;
import com.sqljudge.notification.event.IncomingEvents.ReportResolved;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public final class NotificationDrafts {

    private static final Locale VIETNAMESE = Locale.forLanguageTag("vi-VN");

    private NotificationDrafts() {}

    public static List<NewNotification> of(Commented event) {
        if (event.recipientId() == null || event.recipientId().equals(event.authorId())) {
            return List.of();
        }
        boolean isProblemThread = event.postId() == null;
        return List.of(
                new NewNotification(
                        event.recipientId(),
                        event.eventId(),
                        NotificationType.COMMENT_REPLY,
                        isProblemThread
                                ? "Có người trả lời bình luận của bạn trong thảo luận bài tập"
                                : "Có bình luận mới trong bài viết của bạn",
                        isProblemThread ? null : "/community/" + event.postId()));
    }

    public static List<NewNotification> of(ReportResolved event) {
        if (!"REMOVE".equals(event.action()) || event.authorId() == null) {
            return List.of();
        }
        String target =
                switch (event.targetType()) {
                    case "POST" -> "Bài viết";
                    case "COMMENT" -> "Bình luận";
                    default -> "Nội dung";
                };
        return List.of(
                new NewNotification(
                        event.authorId(),
                        event.eventId(),
                        NotificationType.CONTENT_REMOVED,
                        target + " của bạn đã bị gỡ vì vi phạm quy định cộng đồng",
                        null));
    }

    public static List<NewNotification> of(ProblemPublished event) {
        return List.of(
                new NewNotification(
                        event.authorId(),
                        event.eventId(),
                        NotificationType.PROBLEM_PUBLISHED,
                        "Bài \"" + event.title() + "\" đã được duyệt và xuất bản",
                        "/problems/" + event.slug()));
    }

    public static List<NewNotification> of(RatingUpdated event) {
        NumberFormat numbers = NumberFormat.getIntegerInstance(VIETNAMESE);
        return event.changes().stream()
                .map(
                        change ->
                                new NewNotification(
                                        change.userId(),
                                        event.eventId(),
                                        NotificationType.RATING_CHANGED,
                                        "Rating của bạn "
                                                + (change.delta() >= 0 ? "tăng " : "giảm ")
                                                + numbers.format(Math.abs(change.delta()))
                                                + ", hiện là "
                                                + numbers.format(change.newRating()),
                                        "/contests"))
                .toList();
    }
}
