package com.sqljudge.contest.scoring;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;

public final class IcpcScoring {

    private static final long SOLVED_WEIGHT = 10_000_000L;

    private IcpcScoring() {}

    public static ProblemScore score(List<Attempt> attempts, ScoringRules rules) {
        int wrongAttempts = 0;
        for (Attempt attempt : countedInOrder(attempts)) {
            if (attempt.verdict() == JudgeVerdict.AC) {
                int acceptedMinute =
                        (int) Duration.between(rules.startAt(), attempt.submittedAt()).toMinutes();
                return new ProblemScore(
                        acceptedMinute,
                        wrongAttempts,
                        acceptedMinute + wrongAttempts * rules.penaltyMinutes(),
                        0);
            }
            wrongAttempts++;
        }
        return new ProblemScore(null, wrongAttempts, 0, 0);
    }

    public static ProblemScore publicScore(List<Attempt> attempts, ScoringRules rules) {
        List<Attempt> beforeFreeze =
                attempts.stream()
                        .filter(attempt -> attempt.submittedAt().isBefore(rules.freezeAt()))
                        .toList();
        ProblemScore shown = score(beforeFreeze, rules);
        if (shown.isSolved()) {
            return shown;
        }
        int pending =
                (int)
                        attempts.stream()
                                .filter(Attempt::counts)
                                .filter(
                                        attempt ->
                                                !attempt.submittedAt().isBefore(rules.freezeAt()))
                                .count();
        return shown.withPending(pending);
    }

    public static long rankingScore(int solved, long penaltyMinutes) {
        return solved * SOLVED_WEIGHT - penaltyMinutes;
    }

    private static List<Attempt> countedInOrder(List<Attempt> attempts) {
        return attempts.stream()
                .filter(Attempt::counts)
                .sorted(Comparator.comparing(Attempt::submittedAt))
                .toList();
    }
}
