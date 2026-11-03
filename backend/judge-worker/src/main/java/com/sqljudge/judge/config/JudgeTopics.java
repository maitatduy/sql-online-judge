package com.sqljudge.judge.config;

public final class JudgeTopics {

    public static final String STUDIO_RUN_REQUESTED = "sqljudge.studio.run-requested";
    public static final String STUDIO_RUN_COMPLETED = "sqljudge.studio.run-completed";
    public static final String ANSWER_CHECK_REQUESTED = "sqljudge.studio.answer-check-requested";
    public static final String ANSWER_TESTCASE_CHECKED = "sqljudge.studio.answer-testcase-checked";
    public static final String DEAD_LETTER_SUFFIX = ".dlt";

    private JudgeTopics() {}
}
