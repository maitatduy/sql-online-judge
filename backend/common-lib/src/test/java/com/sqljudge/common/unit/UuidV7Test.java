package com.sqljudge.common.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.common.persistence.UuidV7;
import com.sqljudge.common.persistence.UuidV7IdGenerator;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import org.hibernate.generator.EventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UuidV7Test {

    private static final long EPOCH_MILLIS =
            Instant.parse("2026-10-11T13:05:22.123Z").toEpochMilli();

    @Test
    @DisplayName("UUID sinh ra có version 7 và variant RFC 4122")
    void generate_anyTime_hasVersion7AndRfcVariant() {
        UUID uuid = UuidV7.generate(EPOCH_MILLIS);

        assertThat(uuid.version()).isEqualTo(7);
        assertThat(uuid.variant()).isEqualTo(2);
    }

    @Test
    @DisplayName("48 bit đầu chứa đúng mốc thời gian mili giây")
    void generate_givenTime_embedsTimestamp() {
        UUID uuid = UuidV7.generate(EPOCH_MILLIS);

        assertThat(UuidV7.timestampMillis(uuid)).isEqualTo(EPOCH_MILLIS);
    }

    @Test
    @DisplayName("UUID sinh sau theo thời gian thì lớn hơn khi so sánh dạng chuỗi")
    void generate_laterTime_sortsAfterEarlierTime() {
        UUID earlier = UuidV7.generate(EPOCH_MILLIS);
        UUID later = UuidV7.generate(EPOCH_MILLIS + 1);

        assertThat(later.toString()).isGreaterThan(earlier.toString());
    }

    @Test
    @DisplayName("Sinh nhiều UUID cùng mili giây không bị trùng")
    void generate_sameMillisecond_returnsDistinctValues() {
        Set<UUID> generated = new HashSet<>();
        IntStream.range(0, 10_000).forEach(i -> generated.add(UuidV7.generate(EPOCH_MILLIS)));

        assertThat(generated).hasSize(10_000);
    }

    @Test
    @DisplayName("Generator của Hibernate chỉ sinh id khi insert và trả UUID version 7")
    void idGenerator_insert_returnsUuidV7() {
        UuidV7IdGenerator generator = new UuidV7IdGenerator();

        Object id = generator.generate(null, new Object(), null, EventType.INSERT);

        assertThat(generator.getEventTypes()).containsExactly(EventType.INSERT);
        assertThat(id)
                .isInstanceOfSatisfying(
                        UUID.class, uuid -> assertThat(uuid.version()).isEqualTo(7));
    }
}
