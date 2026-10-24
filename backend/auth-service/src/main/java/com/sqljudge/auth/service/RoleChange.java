package com.sqljudge.auth.service;

import com.sqljudge.common.security.Role;

public record RoleChange(Role role, String reason) {}
