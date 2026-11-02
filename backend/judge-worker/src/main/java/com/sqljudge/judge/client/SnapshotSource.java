package com.sqljudge.judge.client;

import java.util.UUID;

@FunctionalInterface
public interface SnapshotSource {

    DatasetSnapshotResponse snapshotOf(UUID versionId, String expectedChecksum);
}
