package com.sqljudge.judge.sandbox;

import java.util.UUID;

final class RunNames {

    private static final int RUN_NAME_HEX_LENGTH = 28;

    private RunNames() {}

    static String newRunName() {
        return "sb_" + randomHex().substring(0, RUN_NAME_HEX_LENGTH);
    }

    static String randomHex() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
