package com.sqljudge.notification.event;

import java.util.List;
import java.util.UUID;

public final class IncomingEvents {

    private IncomingEvents() {}

    public record Commented(
            UUID eventId, UUID postId, UUID problemId, UUID authorId, UUID recipientId) {}

    public record ReportResolved(
            UUID eventId, String targetType, UUID targetId, UUID authorId, String action) {}

    public record ProblemPublished(UUID eventId, String slug, String title, UUID authorId) {}

    public record RatingUpdated(UUID eventId, List<RatingChange> changes) {}

    public record RatingChange(UUID userId, int newRating, int delta) {}
}
