package com.sqljudge.community.service;

import java.util.UUID;

public record CommentSearch(UUID postId, UUID problemId, CommentSort sort) {}
