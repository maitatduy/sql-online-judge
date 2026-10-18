package com.sqljudge.auth.service;

import com.sqljudge.auth.dto.response.AccountStatus;
import com.sqljudge.common.security.Role;

public record AdminUserSearch(
        String q, Role role, AccountStatus status, Boolean emailVerified, boolean deleted) {}
