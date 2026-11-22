package com.sqljudge.contest.scoring;

import java.time.Instant;

public record Attempt(Instant submittedAt, JudgeVerdict verdict) {

    boolean counts() {
        return verdict == JudgeVerdict.AC || verdict.isPenalized();
    }
}
