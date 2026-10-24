package com.sqljudge.auth.dto.response;

import java.util.List;

public record BulkUserActionResponse(int affectedCount, List<String> skippedUsernames) {

    public BulkUserActionResponse {
        skippedUsernames = List.copyOf(skippedUsernames);
    }
}
