package com.sqljudge.auth.repository;

public record UserCounts(long total, long unverified, long setters, long moderators, long banned) {}
