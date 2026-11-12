package com.sqljudge.judge.sandbox;

import java.time.Instant;

public interface OrphanCleaner {

    int removeRunsCreatedBefore(Instant cutoff);
}
