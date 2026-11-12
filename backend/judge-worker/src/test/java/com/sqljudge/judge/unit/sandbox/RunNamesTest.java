package com.sqljudge.judge.unit.sandbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.judge.sandbox.RunNames;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RunNamesTest {

    private static final Instant CREATED_AT = Instant.parse("2026-11-12T08:15:30.123Z");

    @Test
    @DisplayName("Tên lần chạy có dạng sb_ cùng 28 ký tự hex và ghi lại thời điểm tạo")
    void newRunName_encodesCreationTime() {
        String runName = RunNames.newRunName(CREATED_AT);

        assertThat(runName).matches("sb_[0-9a-f]{28}");
        assertThat(RunNames.createdAtOf(runName)).contains(CREATED_AT);
    }

    @Test
    @DisplayName("Hai lần chạy tạo cùng thời điểm vẫn có tên khác nhau")
    void newRunName_sameInstant_isUnique() {
        assertThat(RunNames.newRunName(CREATED_AT)).isNotEqualTo(RunNames.newRunName(CREATED_AT));
    }

    @ParameterizedTest
    @ValueSource(strings = {"public", "sb_keep", "sb_0192F3B0000070008000000000b8", "sb_0192f3b0"})
    @DisplayName("Tên không phải của một lần chạy thì không có thời điểm tạo")
    void createdAtOf_foreignName_isEmpty(String name) {
        assertThat(RunNames.createdAtOf(name)).isEmpty();
    }
}
