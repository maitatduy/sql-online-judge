package com.sqljudge.contest.scoring;

public enum JudgeVerdict {
    AC,
    WA,
    OM,
    TLE,
    RE,
    FS,
    SE;

    public boolean isPenalized() {
        return this == WA || this == OM || this == TLE;
    }
}
