package com.sqljudge.judge.sandbox;

import java.util.regex.Pattern;

final class SqlIdentifiers {

    private static final Pattern IDENTIFIER = Pattern.compile("[a-z_][a-z0-9_]{0,62}");

    private SqlIdentifiers() {}

    static void requireValid(String name) {
        if (name == null || !IDENTIFIER.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid SQL identifier: " + name);
        }
    }

    static String quoted(String name) {
        return "\"" + name + "\"";
    }
}
