package com.sqljudge.judge.compare;

import java.util.List;

public record ComparisonResult(
        Verdict verdict,
        MismatchReason reason,
        List<Integer> missingRows,
        List<Integer> extraRows,
        Integer firstOrderMismatchRow) {

    public ComparisonResult {
        missingRows = List.copyOf(missingRows);
        extraRows = List.copyOf(extraRows);
    }

    public static ComparisonResult accepted() {
        return new ComparisonResult(Verdict.AC, MismatchReason.NONE, List.of(), List.of(), null);
    }

    public static ComparisonResult columnsDiffer(MismatchReason reason) {
        return new ComparisonResult(Verdict.WA, reason, List.of(), List.of(), null);
    }

    public static ComparisonResult rowsDiffer(List<Integer> missingRows, List<Integer> extraRows) {
        return new ComparisonResult(Verdict.WA, MismatchReason.ROWS, missingRows, extraRows, null);
    }

    public static ComparisonResult orderMismatch(int firstOrderMismatchRow) {
        return new ComparisonResult(
                Verdict.OM, MismatchReason.ORDER, List.of(), List.of(), firstOrderMismatchRow);
    }

    public boolean isAccepted() {
        return verdict == Verdict.AC;
    }
}
