package com.sqljudge.problem.dto.response;

import java.util.List;
import java.util.UUID;

public record TopicProgressResponse(UUID userId, List<TopicProgress> topics) {

    public TopicProgressResponse {
        topics = List.copyOf(topics);
    }

    public record TopicProgress(String slug, String name, long solved, long total) {}
}
