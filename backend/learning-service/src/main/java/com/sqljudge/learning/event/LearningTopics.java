package com.sqljudge.learning.event;

public final class LearningTopics {

    public static final String LESSON_RUN_REQUESTED = "sqljudge.lesson.run-requested";
    public static final String LESSON_RUN_COMPLETED = "sqljudge.lesson.run-completed";
    public static final String DEAD_LETTER_SUFFIX = ".dlt";

    private LearningTopics() {}
}
