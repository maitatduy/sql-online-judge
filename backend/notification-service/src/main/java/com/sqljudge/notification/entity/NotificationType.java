package com.sqljudge.notification.entity;

public enum NotificationType {
    COMMENT_REPLY,
    CONTENT_REMOVED,
    PROBLEM_PUBLISHED,
    RATING_CHANGED,
    ANNOUNCEMENT;

    public boolean isEmailed() {
        return this == CONTENT_REMOVED || this == PROBLEM_PUBLISHED || this == RATING_CHANGED;
    }
}
