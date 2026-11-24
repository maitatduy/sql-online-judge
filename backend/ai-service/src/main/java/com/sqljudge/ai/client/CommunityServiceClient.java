package com.sqljudge.ai.client;

import java.util.List;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "community-service",
        url = "${sqljudge.community-service-url}",
        fallbackFactory = CommunityServiceFallbackFactory.class)
public interface CommunityServiceClient {

    @GetMapping("/api/community/posts/{postId}")
    CommunityPost post(@PathVariable("postId") UUID postId);

    @GetMapping("/api/community/comments?sort=TOP&page=1&size=50")
    CommentPage comments(@RequestParam("postId") UUID postId);

    record CommunityPost(String title, String bodyMarkdown) {}

    record CommunityComment(String authorUsername, String body, List<CommunityComment> replies) {}

    record CommentPage(List<CommunityComment> items) {}
}
