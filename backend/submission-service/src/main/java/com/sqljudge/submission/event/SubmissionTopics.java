package com.sqljudge.submission.event;

public final class SubmissionTopics {

    public static final String SUBMISSION_CREATED = "sqljudge.submission.created";
    public static final String SUBMISSION_JUDGED = "sqljudge.submission.judged";
    public static final String JUDGE_PROGRESS = "sqljudge.judge.progress";
    public static final String JUDGE_COMPLETED = "sqljudge.judge.completed";
    public static final String DEAD_LETTER_SUFFIX = ".dlt";

    private SubmissionTopics() {}
}
