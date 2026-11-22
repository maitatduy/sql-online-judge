package com.sqljudge.contest.event;

public final class ContestTopics {

    public static final String CONTEST_STARTED = "sqljudge.contest.started";
    public static final String CONTEST_FINISHED = "sqljudge.contest.finished";
    public static final String SUBMISSION_JUDGED = "sqljudge.submission.judged";
    public static final String DEAD_LETTER_SUFFIX = ".dlt";

    private ContestTopics() {}
}
