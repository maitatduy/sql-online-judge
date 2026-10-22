package com.sqljudge.auth.entity;

public record RestrictionsChange(String previous, String current, String reason) {}
