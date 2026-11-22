package com.sqljudge.contest.scoring;

public record ProblemScore(
        Integer acceptedMinute, int wrongAttempts, int penaltyMinutes, int pendingAttempts) {

    public boolean isSolved() {
        return acceptedMinute != null;
    }

    ProblemScore withPending(int pending) {
        return new ProblemScore(acceptedMinute, wrongAttempts, penaltyMinutes, pending);
    }
}
