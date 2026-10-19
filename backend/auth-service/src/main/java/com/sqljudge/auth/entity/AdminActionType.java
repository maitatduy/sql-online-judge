package com.sqljudge.auth.entity;

public enum AdminActionType {
    BAN,
    UNBAN,
    REVOKE_SESSION,
    REVOKE_ALL_SESSIONS,
    UPDATE_ROLE,
    UPDATE_DISPLAY_NAME,
    SEND_PASSWORD_RESET,
    SEND_VERIFICATION
}
