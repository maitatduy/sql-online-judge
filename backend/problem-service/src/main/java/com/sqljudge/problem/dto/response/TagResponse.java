package com.sqljudge.problem.dto.response;

import com.sqljudge.problem.entity.Tag;

public record TagResponse(String slug, String name) {

    public static TagResponse from(Tag tag) {
        return new TagResponse(tag.getSlug(), tag.getName());
    }
}
