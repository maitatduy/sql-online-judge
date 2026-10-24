package com.sqljudge.judge.compare;

import java.util.List;

public record ComparisonResult(
        Verdict verdict, MismatchReason reason, List<Row> missingRows, List<Row> extraRows) {

    public ComparisonResult {
        missingRows = List.copyOf(missingRows);
        extraRows = List.copyOf(extraRows);
    }

    public static ComparisonResult accepted() {
        return new ComparisonResult(Verdict.AC, MismatchReason.NONE, List.of(), List.of());
    }

    public static ComparisonResult rejected(Verdict verdict, MismatchReason reason) {
        return new ComparisonResult(verdict, reason, List.of(), List.of());
    }

    public static ComparisonResult rowsDiffer(List<Row> missingRows, List<Row> extraRows) {
        return new ComparisonResult(Verdict.WA, MismatchReason.ROWS, missingRows, extraRows);
    }

    public boolean isAccepted() {
        return verdict == Verdict.AC;
    }
}
