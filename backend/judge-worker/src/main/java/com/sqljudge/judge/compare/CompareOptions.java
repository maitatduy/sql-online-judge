package com.sqljudge.judge.compare;

import java.math.BigDecimal;

public record CompareOptions(boolean ordered, boolean compareColumnNames, BigDecimal tolerance) {

    public static final BigDecimal DEFAULT_TOLERANCE = new BigDecimal("1e-6");

    public static CompareOptions defaults() {
        return new CompareOptions(false, false, DEFAULT_TOLERANCE);
    }

    public CompareOptions withOrdered(boolean value) {
        return new CompareOptions(value, compareColumnNames, tolerance);
    }

    public CompareOptions withCompareColumnNames(boolean value) {
        return new CompareOptions(ordered, value, tolerance);
    }

    public CompareOptions withTolerance(BigDecimal value) {
        return new CompareOptions(ordered, compareColumnNames, value);
    }
}
