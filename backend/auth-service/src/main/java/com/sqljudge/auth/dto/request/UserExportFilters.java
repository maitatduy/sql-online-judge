package com.sqljudge.auth.dto.request;

import com.sqljudge.auth.dto.response.AccountStatus;
import com.sqljudge.common.security.Role;
import jakarta.validation.constraints.Size;

public record UserExportFilters(
        @Size(max = 254, message = "Từ khoá quá dài") String q,
        Role role,
        AccountStatus status,
        Boolean emailVerified,
        Boolean deleted) {}
