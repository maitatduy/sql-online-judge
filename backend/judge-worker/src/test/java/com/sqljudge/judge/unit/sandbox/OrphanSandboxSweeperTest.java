package com.sqljudge.judge.unit.sandbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.judge.sandbox.OrphanCleaner;
import com.sqljudge.judge.sandbox.OrphanSandboxSweeper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrphanSandboxSweeperTest {

    private static final Instant NOW = Instant.parse("2026-11-12T08:00:00Z");
    private static final Duration ORPHAN_AGE = Duration.ofMinutes(10);

    @Test
    @DisplayName("Mỗi sandbox được dọn các lần chạy tạo trước mốc bây giờ trừ tuổi mồ côi")
    void sweep_passesCutoffToEveryCleaner() {
        List<Instant> cutoffs = new ArrayList<>();
        OrphanCleaner recording =
                cutoff -> {
                    cutoffs.add(cutoff);
                    return 0;
                };

        sweeperOf(List.of(recording, recording)).sweep();

        assertThat(cutoffs).containsExactly(NOW.minus(ORPHAN_AGE), NOW.minus(ORPHAN_AGE));
    }

    @Test
    @DisplayName("Một sandbox dọn lỗi không chặn việc dọn các sandbox còn lại")
    void sweep_failingCleaner_doesNotStopOthers() {
        List<Instant> cutoffs = new ArrayList<>();
        OrphanCleaner failing =
                cutoff -> {
                    throw new IllegalStateException("sandbox down");
                };
        OrphanCleaner recording =
                cutoff -> {
                    cutoffs.add(cutoff);
                    return 1;
                };

        sweeperOf(List.of(failing, recording)).sweep();

        assertThat(cutoffs).containsExactly(NOW.minus(ORPHAN_AGE));
    }

    private static OrphanSandboxSweeper sweeperOf(List<OrphanCleaner> cleaners) {
        return new OrphanSandboxSweeper(cleaners, Clock.fixed(NOW, ZoneOffset.UTC), ORPHAN_AGE);
    }
}
