package com.sqljudge.judge.job;

import com.sqljudge.judge.compare.Verdict;
import com.sqljudge.judge.sandbox.Dialect;
import java.util.List;
import java.util.UUID;

public record StudioRunResult(
        UUID runId,
        Dialect dialect,
        Rejection rejection,
        String systemError,
        List<VariantRunResult> variants) {

    public StudioRunResult {
        variants = List.copyOf(variants);
    }

    public record Rejection(Verdict verdict, String message) {}

    static StudioRunResult completed(StudioRunRequest request, List<VariantRunResult> variants) {
        return new StudioRunResult(request.runId(), request.dialect(), null, null, variants);
    }

    static StudioRunResult rejected(StudioRunRequest request, Rejection rejection) {
        return new StudioRunResult(request.runId(), request.dialect(), rejection, null, List.of());
    }

    static StudioRunResult systemError(StudioRunRequest request, String reason) {
        return new StudioRunResult(request.runId(), request.dialect(), null, reason, List.of());
    }
}
