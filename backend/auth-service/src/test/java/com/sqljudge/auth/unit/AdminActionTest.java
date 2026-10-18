package com.sqljudge.auth.unit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sqljudge.auth.entity.AdminAction;
import com.sqljudge.auth.entity.User;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class AdminActionTest {

    private static final UUID ADMIN_ID = UUID.fromString("0192f3a0-0000-7000-8000-00000000000a");

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("Khoá và mở khoá bắt buộc có lý do, kể cả khi gọi không qua API")
    void banAndUnban_withoutReason_areRejected(String reason) {
        User target = new User();

        assertThatThrownBy(() -> AdminAction.ban(ADMIN_ID, target, reason))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AdminAction.unban(ADMIN_ID, target, reason))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
