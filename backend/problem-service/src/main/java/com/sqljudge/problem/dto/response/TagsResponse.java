package com.sqljudge.problem.dto.response;

import java.util.List;

public record TagsResponse(List<TagResponse> items) {

    public TagsResponse {
        items = List.copyOf(items);
    }
}
