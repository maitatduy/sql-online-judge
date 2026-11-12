package com.sqljudge.judge.sandbox;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RunNames {

    private static final String PREFIX = "sb_";
    private static final int TIME_HEX_LENGTH = 12;
    private static final int RANDOM_HEX_LENGTH = 16;
    private static final Pattern RUN_NAME =
            Pattern.compile(
                    PREFIX
                            + "([0-9a-f]{"
                            + TIME_HEX_LENGTH
                            + "})[0-9a-f]{"
                            + RANDOM_HEX_LENGTH
                            + "}");

    private RunNames() {}

    public static String newRunName(Instant createdAt) {
        String time = "%0" + TIME_HEX_LENGTH + "x";
        return PREFIX
                + time.formatted(createdAt.toEpochMilli())
                + randomHex().substring(0, RANDOM_HEX_LENGTH);
    }

    public static Optional<Instant> createdAtOf(String runName) {
        Matcher matcher = RUN_NAME.matcher(runName);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(Instant.ofEpochMilli(Long.parseLong(matcher.group(1), 16)));
    }

    static boolean wasCreatedBefore(String runName, Instant cutoff) {
        return createdAtOf(runName).filter(createdAt -> createdAt.isBefore(cutoff)).isPresent();
    }

    static String randomHex() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
