package com.sqljudge.judge.client;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SnapshotProvider implements SnapshotSource {

    private static final int CACHED_SNAPSHOTS = 16;

    private final ProblemServiceClient problemServiceClient;
    private final Map<UUID, DatasetSnapshotResponse> cache =
            Collections.synchronizedMap(new BoundedCache());

    public SnapshotProvider(ProblemServiceClient problemServiceClient) {
        this.problemServiceClient = problemServiceClient;
    }

    @Override
    public DatasetSnapshotResponse snapshotOf(UUID versionId, String expectedChecksum) {
        DatasetSnapshotResponse cached = cache.get(versionId);
        if (cached != null && cached.checksum().equals(expectedChecksum)) {
            return cached;
        }
        DatasetSnapshotResponse fetched = problemServiceClient.snapshot(versionId);
        if (!fetched.checksum().equals(expectedChecksum)) {
            throw new SnapshotUnavailableException(
                    "Dataset checksum mismatch for version " + versionId, null);
        }
        cache.put(versionId, fetched);
        return fetched;
    }

    private static final class BoundedCache extends LinkedHashMap<UUID, DatasetSnapshotResponse> {

        private BoundedCache() {
            super(CACHED_SNAPSHOTS, 0.75f, true);
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, DatasetSnapshotResponse> eldest) {
            return size() > CACHED_SNAPSHOTS;
        }
    }
}
