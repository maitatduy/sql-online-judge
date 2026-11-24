package com.sqljudge.notification.event;

public final class NotificationTopics {

    public static final String COMMENTED = "sqljudge.community.commented";
    public static final String REPORT_RESOLVED = "sqljudge.community.report-resolved";
    public static final String PROBLEM_PUBLISHED = "sqljudge.problem.published";
    public static final String RATING_UPDATED = "sqljudge.contest.rating-updated";
    public static final String DEAD_LETTER_SUFFIX = ".dlt";

    private NotificationTopics() {}
}
