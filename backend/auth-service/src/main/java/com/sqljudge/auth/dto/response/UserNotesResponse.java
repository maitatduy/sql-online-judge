package com.sqljudge.auth.dto.response;

import java.util.List;

public record UserNotesResponse(List<UserNoteResponse> items) {

    public UserNotesResponse {
        items = List.copyOf(items);
    }
}
