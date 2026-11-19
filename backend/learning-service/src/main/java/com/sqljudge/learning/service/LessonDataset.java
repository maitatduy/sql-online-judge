package com.sqljudge.learning.service;

import java.util.UUID;

public record LessonDataset(UUID variantId, UUID versionId, String checksum) {}
