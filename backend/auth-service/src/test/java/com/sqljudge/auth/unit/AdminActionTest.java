package com.sqljudge.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sqljudge.auth.entity.AdminAction;
import com.sqljudge.auth.entity.AdminActionType;
import com.sqljudge.auth.entity.User;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class AdminActionTest {

    private static final UUID ADMIN_ID = UUID.fromString("0192f3a0-0000-7000-8000-00000000000a");

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("Khoá, mở khoá, xuất email bắt buộc có lý do, kể cả khi gọi không qua API")
    void banAndUnban_withoutReason_areRejected(String reason) {
        User target = new User();

        assertThatThrownBy(() -> AdminAction.ban(ADMIN_ID, target, reason))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AdminAction.unban(ADMIN_ID, target, reason))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AdminAction.exportUserEmails(ADMIN_ID, reason, "scope=ALL"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Nhật ký xuất email không gắn với tài khoản nào, chỉ ghi lý do và chi tiết")
    void exportUserEmails_hasNoTargetUser() {
        AdminAction action = AdminAction.exportUserEmails(ADMIN_ID, "Khảo sát", "scope=ALL;rows=3");

        assertThat(action.getAction()).isEqualTo(AdminActionType.EXPORT_USER_EMAILS);
        assertThat(action.getActorId()).isEqualTo(ADMIN_ID);
        assertThat(action.getTargetUserId()).isNull();
        assertThat(action.getReason()).isEqualTo("Khảo sát");
        assertThat(action.getDetails()).isEqualTo("scope=ALL;rows=3");
    }
}
