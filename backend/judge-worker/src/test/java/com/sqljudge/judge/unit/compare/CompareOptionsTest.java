package com.sqljudge.judge.unit.compare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sqljudge.judge.compare.CompareOptions;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CompareOptionsTest {

    @Test
    @DisplayName("Sai số null bị từ chối")
    void create_nullTolerance_throws() {
        assertThatThrownBy(() -> new CompareOptions(false, false, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Sai số âm bị từ chối")
    void create_negativeTolerance_throws() {
        assertThatThrownBy(() -> new CompareOptions(false, false, new BigDecimal("-1e-6")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Sai số 0 được chấp nhận")
    void create_zeroTolerance_isAllowed() {
        assertThat(new CompareOptions(true, true, BigDecimal.ZERO).tolerance()).isZero();
    }
}
