package com.sqljudge.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.auth.dto.response.AuditLogEntryResponse;
import com.sqljudge.auth.entity.AdminActionType;
import com.sqljudge.auth.service.AuditLogCsv;
import com.sqljudge.common.security.Role;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuditLogCsvTest {

    private static final UUID ID = UUID.fromString("0192f3a0-0000-7000-8000-0000000000e1");
    private static final Instant AT = Instant.parse("2026-10-13T07:05:12Z");

    @Test
    @DisplayName("Dòng tiêu đề liệt kê đủ cột theo thứ tự cố định")
    void header_listsColumns() {
        assertThat(AuditLogCsv.header())
                .isEqualTo(
                        "occurredAt,actor,actorRole,action,target,previousValue,newValue,reason,"
                                + "expiresAt,details\n");
    }

    @Test
    @DisplayName("Giá trị rỗng thành ô trống, dấu phẩy được bọc ngoặc kép")
    void row_blankForNullAndQuotesCommas() {
        AuditLogEntryResponse entry =
                new AuditLogEntryResponse(
                        ID,
                        AT,
                        "admin.huy",
                        Role.ADMIN,
                        "datahust",
                        AdminActionType.UPDATE_ROLE,
                        "CLB, được duyệt",
                        "LEARNER",
                        "SETTER",
                        null,
                        null);

        assertThat(AuditLogCsv.row(entry))
                .isEqualTo(
                        "2026-10-13T07:05:12Z,admin.huy,ADMIN,UPDATE_ROLE,datahust,LEARNER,SETTER,"
                                + "\"CLB, được duyệt\",,\n");
    }

    @Test
    @DisplayName("Lý do bắt đầu bằng ký tự công thức được thêm tiền tố ' để chống CSV injection")
    void row_guardsFormulaValues() {
        AuditLogEntryResponse entry =
                new AuditLogEntryResponse(
                        ID,
                        AT,
                        "admin.huy",
                        Role.ADMIN,
                        null,
                        AdminActionType.EXPORT_USER_EMAILS,
                        "=HYPERLINK(\"http://x\")",
                        null,
                        null,
                        null,
                        "12 tài khoản");

        assertThat(AuditLogCsv.row(entry))
                .isEqualTo(
                        "2026-10-13T07:05:12Z,admin.huy,ADMIN,EXPORT_USER_EMAILS,,,,"
                                + "\"'=HYPERLINK(\"\"http://x\"\")\",,12 tài khoản\n");
    }

    @Test
    @DisplayName("Tên file theo thời điểm xuất (UTC)")
    void fileName_usesUtcTimestamp() {
        assertThat(AuditLogCsv.fileName(Instant.parse("2026-10-25T05:30:59Z")))
                .isEqualTo("audit-log-20261025-0530.csv");
    }
}
