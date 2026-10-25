package com.sqljudge.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sqljudge.auth.dto.response.AuditLogEntryResponse;
import com.sqljudge.auth.entity.AdminAction;
import com.sqljudge.auth.entity.AdminActionType;
import com.sqljudge.auth.entity.AuditCategory;
import com.sqljudge.auth.repository.AdminActionRepository;
import com.sqljudge.auth.service.AdminAuditLogService;
import com.sqljudge.auth.service.AuditLogSearch;
import com.sqljudge.common.security.Role;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;

@ExtendWith(MockitoExtension.class)
class AdminAuditLogServiceTest {

    private static final int BATCH_SIZE = 500;
    private static final UUID ADMIN_ID = UUID.fromString("0192f3a0-0000-7000-8000-00000000000a");
    private static final Instant NEWEST = Instant.parse("2026-10-25T05:00:00Z");
    private static final AuditLogSearch NO_FILTER =
            new AuditLogSearch(null, null, null, null, null, null);

    @Mock private AdminActionRepository adminActionRepository;

    private AdminAuditLogService service;

    @BeforeEach
    void setUp() {
        service = new AdminAuditLogService(adminActionRepository);
    }

    @Test
    @DisplayName("Xuất hơn một đợt: đợt sau lấy tiếp từ con trỏ của dòng cuối, không trùng dòng")
    void writeCsv_moreThanOneBatch_continuesFromLastRowCursor() throws Exception {
        List<AuditLogEntryResponse> firstBatch = entries(0, BATCH_SIZE);
        AuditLogEntryResponse last = firstBatch.get(BATCH_SIZE - 1);
        List<AuditLogEntryResponse> secondBatch = entries(BATCH_SIZE, 1);
        when(batchAfter(null, null)).thenReturn(firstBatch);
        when(batchAfter(last.occurredAt(), last.id())).thenReturn(secondBatch);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.writeCsv(NO_FILTER, out);

        String[] lines = out.toString(StandardCharsets.UTF_8).split("\n");
        assertThat(lines).hasSize(1 + BATCH_SIZE + 1);
        assertThat(lines).doesNotHaveDuplicates();
        verify(adminActionRepository, times(2))
                .findAuditLogBatch(
                        any(), any(), any(), any(), any(), any(), any(), any(Limit.class));
    }

    @Test
    @DisplayName("Đợt đầu chưa đủ cỡ đợt thì dừng ngay, chỉ một câu truy vấn")
    void writeCsv_shortFirstBatch_queriesOnce() throws Exception {
        when(batchAfter(null, null)).thenReturn(entries(0, 3));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.writeCsv(NO_FILTER, out);

        assertThat(out.toString(StandardCharsets.UTF_8).split("\n")).hasSize(4);
        verify(adminActionRepository, times(1))
                .findAuditLogBatch(
                        any(), any(), any(), any(), any(), any(), any(), any(Limit.class));
    }

    @Test
    @DisplayName("Ghi nhật ký lần xuất: loại EXPORT_AUDIT_LOG, người xuất và bộ lọc đã dùng")
    void recordExport_savesActionWithFilters() {
        service.recordExport(
                new AuditLogSearch(
                        "viet.luu",
                        "admin.huy",
                        AdminActionType.BAN,
                        AuditCategory.ACCOUNT,
                        " spam ",
                        Instant.parse("2026-10-18T05:30:00Z")),
                ADMIN_ID);

        AdminAction saved = savedAction();
        assertThat(saved.getAction()).isEqualTo(AdminActionType.EXPORT_AUDIT_LOG);
        assertThat(saved.getActorId()).isEqualTo(ADMIN_ID);
        assertThat(saved.getTargetUserId()).isNull();
        assertThat(saved.getDetails())
                .isEqualTo(
                        "user=viet.luu; actor=admin.huy; action=BAN; category=ACCOUNT; q=spam;"
                                + " from=2026-10-18T05:30:00Z");
    }

    @Test
    @DisplayName("Xuất không lọc gì thì ghi rõ là toàn bộ nhật ký")
    void recordExport_noFilter_describesWholeLog() {
        service.recordExport(NO_FILTER, ADMIN_ID);

        assertThat(savedAction().getDetails()).isEqualTo("Toàn bộ nhật ký");
    }

    private List<AuditLogEntryResponse> batchAfter(Instant beforeAt, UUID beforeId) {
        return adminActionRepository.findAuditLogBatch(
                isNull(),
                isNull(),
                eq(List.of(AdminActionType.values())),
                isNull(),
                isNull(),
                beforeAt == null ? isNull() : eq(beforeAt),
                beforeId == null ? isNull() : eq(beforeId),
                eq(Limit.of(BATCH_SIZE)));
    }

    private AdminAction savedAction() {
        ArgumentCaptor<AdminAction> saved = ArgumentCaptor.forClass(AdminAction.class);
        verify(adminActionRepository).save(saved.capture());
        return saved.getValue();
    }

    private static List<AuditLogEntryResponse> entries(int firstIndex, int count) {
        return IntStream.range(firstIndex, firstIndex + count)
                .mapToObj(
                        index ->
                                new AuditLogEntryResponse(
                                        UUID.nameUUIDFromBytes(
                                                String.valueOf(index)
                                                        .getBytes(StandardCharsets.UTF_8)),
                                        NEWEST.minusSeconds(index),
                                        "admin.huy",
                                        Role.ADMIN,
                                        "user" + index,
                                        AdminActionType.SEND_VERIFICATION,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null))
                .toList();
    }
}
