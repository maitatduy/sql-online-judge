package com.sqljudge.auth.entity;

import java.util.Arrays;
import java.util.List;

public enum AdminActionType {
    BAN,
    UNBAN,
    REVOKE_SESSION,
    REVOKE_ALL_SESSIONS,
    UPDATE_ROLE,
    UPDATE_DISPLAY_NAME,
    SEND_PASSWORD_RESET,
    SEND_VERIFICATION,
    DELETE,
    RESTORE,
    UPDATE_AVATAR,
    REMOVE_AVATAR,
    EXPORT_USER_EMAILS,
    UPDATE_RESTRICTIONS;

    public static List<AdminActionType> inCategory(AuditCategory category) {
        return Arrays.stream(values()).filter(type -> type.category() == category).toList();
    }

    public AuditCategory category() {
        return AuditCategory.ACCOUNT;
    }
}
