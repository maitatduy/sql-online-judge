package com.sqljudge.community.dto.response;

import com.sqljudge.community.entity.PostCategory;
import java.util.Map;

public record CommunityOverviewResponse(long totalPosts, Map<PostCategory, Long> categoryCounts) {}
