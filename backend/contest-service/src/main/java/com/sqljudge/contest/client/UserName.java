package com.sqljudge.contest.client;

import java.util.UUID;

public record UserName(UUID userId, String username, String displayName) {}
