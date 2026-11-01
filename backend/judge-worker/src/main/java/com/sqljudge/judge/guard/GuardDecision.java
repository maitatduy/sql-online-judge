package com.sqljudge.judge.guard;

import com.sqljudge.judge.compare.SanitizedMessage;
import com.sqljudge.judge.compare.Verdict;

public sealed interface GuardDecision {

    record Allowed() implements GuardDecision {}

    record Unverified() implements GuardDecision {}

    record Rejected(Verdict verdict, SanitizedMessage message) implements GuardDecision {}
}
