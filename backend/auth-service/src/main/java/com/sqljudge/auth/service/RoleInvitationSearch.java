package com.sqljudge.auth.service;

import com.sqljudge.auth.entity.InvitationStatus;

public record RoleInvitationSearch(InvitationStatus status, String keyword) {}
