package com.sqljudge.judge.compare;

import java.math.BigDecimal;
import java.util.Objects;

public record CompareOptions(boolean ordered, boolean compareColumnNames, BigDecimal tolerance) {

    public static final BigDecimal DEFAULT_TOLERANCE = new BigDecimal("1e-6");

    public CompareOptions {
        Objects.requireNonNull(tolerance, "tolerance");
        if (tolerance.signum() < 0) {
            throw new IllegalArgumentException("tolerance must not be negative");
        }
    }
}
