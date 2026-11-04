package com.sqljudge.problem.event;

public final class StudioTopics {

    public static final String RUN_REQUESTED = "sqljudge.studio.run-requested";
    public static final String RUN_COMPLETED = "sqljudge.studio.run-completed";
    public static final String ANSWER_CHECK_REQUESTED = "sqljudge.studio.answer-check-requested";
    public static final String ANSWER_TESTCASE_CHECKED = "sqljudge.studio.answer-testcase-checked";
    public static final String DEAD_LETTER_SUFFIX = ".dlt";

    private StudioTopics() {}
}
