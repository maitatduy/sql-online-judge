package com.sqljudge.judge.unit.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sqljudge.judge.client.DatasetSnapshotResponse;
import com.sqljudge.judge.client.JudgePackageResponse;
import com.sqljudge.judge.client.ProblemServiceClient;
import com.sqljudge.judge.client.SnapshotProvider;
import com.sqljudge.judge.client.SnapshotUnavailableException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SnapshotProviderTest {

    private static final UUID VERSION_ID = UUID.randomUUID();
    private static final String CHECKSUM = "a".repeat(64);

    @Test
    @DisplayName("Snapshot khớp checksum được dùng lại từ cache, không tải lại")
    void snapshotOf_matchingChecksum_isCached() {
        CountingClient client = new CountingClient(CHECKSUM);
        SnapshotProvider provider = new SnapshotProvider(client);

        provider.snapshotOf(VERSION_ID, CHECKSUM);
        DatasetSnapshotResponse second = provider.snapshotOf(VERSION_ID, CHECKSUM);

        assertThat(second.checksum()).isEqualTo(CHECKSUM);
        assertThat(client.fetches.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Dataset chưa chốt (không có checksum) thì báo không dùng được, không ném NPE")
    void snapshotOf_missingChecksum_isUnavailable() {
        SnapshotProvider provider = new SnapshotProvider(new CountingClient(null));

        assertThatThrownBy(() -> provider.snapshotOf(VERSION_ID, null))
                .isInstanceOf(SnapshotUnavailableException.class);
        assertThatThrownBy(() -> provider.snapshotOf(VERSION_ID, CHECKSUM))
                .isInstanceOf(SnapshotUnavailableException.class);
    }

    @Test
    @DisplayName("Checksum lệch với gói chấm thì báo không dùng được")
    void snapshotOf_checksumMismatch_isUnavailable() {
        SnapshotProvider provider = new SnapshotProvider(new CountingClient("b".repeat(64)));

        assertThatThrownBy(() -> provider.snapshotOf(VERSION_ID, CHECKSUM))
                .isInstanceOf(SnapshotUnavailableException.class);
    }

    private static final class CountingClient implements ProblemServiceClient {

        private final String checksum;
        private final AtomicInteger fetches = new AtomicInteger();

        private CountingClient(String checksum) {
            this.checksum = checksum;
        }

        @Override
        public DatasetSnapshotResponse snapshot(UUID versionId) {
            fetches.incrementAndGet();
            return new DatasetSnapshotResponse(versionId, checksum, List.of(), List.of());
        }

        @Override
        public JudgePackageResponse judgePackage(UUID revisionId) {
            throw new UnsupportedOperationException();
        }
    }
}
