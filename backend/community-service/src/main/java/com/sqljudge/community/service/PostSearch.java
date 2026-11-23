package com.sqljudge.community.service;

import com.sqljudge.community.entity.PostCategory;
import java.util.UUID;

public record PostSearch(
        PostCategory category, PostTab tab, String keyword, String tag, UUID problemId) {}
