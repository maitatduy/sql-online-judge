package com.sqljudge.auth.service;

final class LikePatterns {

    static final char ESCAPE = '\\';

    private LikePatterns() {}

    static String containing(String keyword) {
        String escaped =
                keyword.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
