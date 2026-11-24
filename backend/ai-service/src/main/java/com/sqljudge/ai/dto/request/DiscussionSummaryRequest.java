package com.sqljudge.ai.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record DiscussionSummaryRequest(@NotNull(message = "Chọn bài viết") UUID postId) {}
