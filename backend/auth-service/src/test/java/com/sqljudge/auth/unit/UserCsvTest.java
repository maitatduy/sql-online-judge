package com.sqljudge.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.service.UserCsv;
import com.sqljudge.common.security.Role;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

class UserCsvTest {

    private static final Instant NOW = Instant.parse("2026-10-20T05:00:00Z");
    private static final Instant JOINED_AT = Instant.parse("2026-03-01T08:30:00Z");

    @Test
    @DisplayName("Tiêu đề không có cột email khi không xuất kèm email")
    void header_withoutEmail() {
        assertThat(UserCsv.header(false)).isEqualTo("username,role,status,joinedAt\n");
        assertThat(UserCsv.header(true)).isEqualTo("username,email,role,status,joinedAt\n");
    }

    @Test
    @DisplayName(
            "Một dòng gồm username, vai trò, trạng thái tại thời điểm xuất và ngày tham gia UTC")
    void row_activeAndBannedUsers() {
        User active = user("viet.luu", "viet.luu@gmail.com");
        User banned = user("an.bui", "an.bui@gmail.com");
        banned.ban(NOW.minus(Duration.ofDays(1)), null, "Spam");

        assertThat(UserCsv.row(active, false, NOW))
                .isEqualTo("viet.luu,SETTER,ACTIVE,2026-03-01T08:30:00Z\n");
        assertThat(UserCsv.row(banned, true, NOW))
                .isEqualTo("an.bui,an.bui@gmail.com,SETTER,BANNED,2026-03-01T08:30:00Z\n");
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(
            delimiter = '|',
            quoteCharacter = '`',
            value = {
                "=HYPERLINK(1)@x.com|'=HYPERLINK(1)@x.com",
                "+1@x.com|'+1@x.com",
                "-1@x.com|'-1@x.com",
                "@a@x.com|'@a@x.com",
                "a,b@x.com|`\"a,b@x.com\"`",
                "a\"b@x.com|`\"a\"\"b@x.com\"`",
                "=a,b@x.com|`\"'=a,b@x.com\"`"
            })
    @DisplayName("Chống CSV injection và giữ đúng định dạng khi giá trị có dấu phẩy, dấu nháy")
    void row_escapesDangerousValues(String email, String expectedCell) {
        User user = user("viet.luu", email);

        assertThat(UserCsv.row(user, true, NOW))
                .isEqualTo("viet.luu," + expectedCell + ",SETTER,ACTIVE,2026-03-01T08:30:00Z\n");
    }

    @Test
    @DisplayName("Giá trị bắt đầu bằng tab được thêm tiền tố ' để chống CSV injection")
    void row_leadingTab_isGuarded() {
        User user = user("viet.luu", "\t=1+1");

        assertThat(UserCsv.row(user, true, NOW))
                .isEqualTo("viet.luu,'\t=1+1,SETTER,ACTIVE,2026-03-01T08:30:00Z\n");
    }

    @Test
    @DisplayName("Giá trị bắt đầu bằng CR được thêm tiền tố ' và đặt trong dấu nháy kép")
    void row_leadingCarriageReturn_isGuardedAndQuoted() {
        User user = user("viet.luu", "\r=1+1");

        assertThat(UserCsv.row(user, true, NOW))
                .isEqualTo("viet.luu,\"'\r=1+1\",SETTER,ACTIVE,2026-03-01T08:30:00Z\n");
    }

    private static User user(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setRole(Role.SETTER);
        ReflectionTestUtils.setField(user, "createdAt", JOINED_AT);
        return user;
    }
}
