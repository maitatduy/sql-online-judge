package com.sqljudge.auth.validation;

import java.util.Set;

public final class UsernameRules {

    public static final String PATTERN = "^[a-z0-9._]{3,30}$";
    public static final String PATTERN_MESSAGE =
            "Tên người dùng gồm 3-30 ký tự: chữ thường, số, dấu chấm hoặc gạch dưới";

    private static final Set<String> RESERVED = Set.of("me", "availability");

    private UsernameRules() {}

    public static boolean isReserved(String username) {
        return RESERVED.contains(username);
    }
}
