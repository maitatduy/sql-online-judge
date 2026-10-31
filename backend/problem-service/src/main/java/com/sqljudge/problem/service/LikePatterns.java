package com.sqljudge.problem.service;

import java.util.Locale;
import java.util.regex.Pattern;

final class LikePatterns {

    private static final Pattern LIKE_SPECIAL_CHARACTERS = Pattern.compile("([!%_])");

    private LikePatterns() {}

    static String containingIgnoreCase(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String lowered = keyword.trim().toLowerCase(Locale.ROOT);
        return "%" + LIKE_SPECIAL_CHARACTERS.matcher(lowered).replaceAll("!$1") + "%";
    }
}
