package com.sqljudge.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.sqljudge.auth.entity.SignupProvider;
import java.util.List;

public record AdminUserDetailResponse(
        @JsonUnwrapped AdminUserSummaryResponse account,
        String displayName,
        String school,
        SignupProvider signupProvider,
        int submissionCount,
        int contestCount,
        List<UserViolationResponse> violations) {}
