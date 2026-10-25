package com.sqljudge.auth.dto.response;

import java.util.List;

public record AuditActorsResponse(List<String> usernames) {

    public AuditActorsResponse {
        usernames = List.copyOf(usernames);
    }
}
