package com.sqljudge.auth.dto.response;

import java.util.List;

public record InviteRolesResponse(int sentCount, List<SkippedInvitation> skipped) {

    public InviteRolesResponse {
        skipped = List.copyOf(skipped);
    }
}
