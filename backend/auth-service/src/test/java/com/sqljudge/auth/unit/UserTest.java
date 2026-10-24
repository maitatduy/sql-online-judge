package com.sqljudge.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.auth.entity.User;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserTest {

    private static final Instant FIRST_DELETION = Instant.parse("2026-10-20T03:00:00Z");

    @Test
    @DisplayName("Đánh dấu xoá lần hai giữ nguyên thời điểm xoá đầu tiên")
    void markDeleted_twice_keepsFirstDeletionTime() {
        User user = new User();

        user.markDeleted(FIRST_DELETION);
        user.markDeleted(FIRST_DELETION.plusSeconds(3600));

        assertThat(user.getDeletedAt()).isEqualTo(FIRST_DELETION);
    }

    @Test
    @DisplayName("Khôi phục bỏ dấu xoá, tài khoản không còn bị coi là đã xoá")
    void restore_deletedUser_clearsDeletion() {
        User user = new User();
        user.markDeleted(FIRST_DELETION);

        user.restore();

        assertThat(user.isDeleted()).isFalse();
        assertThat(user.getDeletedAt()).isNull();
    }
}
