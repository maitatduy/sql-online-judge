package com.sqljudge.auth.entity;

import java.time.Instant;

public record InvitationLink(String tokenHash, Instant expiresAt) {}
