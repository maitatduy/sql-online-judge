package com.sqljudge.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.sqljudge.auth.config.AdminProperties;
import com.sqljudge.auth.dto.request.ExportScope;
import com.sqljudge.auth.dto.request.ExportUsersRequest;
import com.sqljudge.auth.entity.AdminAction;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.repository.AdminActionRepository;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.auth.service.AdminUserExportService;
import com.sqljudge.auth.service.UserExport;
import com.sqljudge.common.security.Role;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class AdminUserExportServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-20T05:00:00Z");
    private static final UUID ADMIN_ID = UUID.fromString("0192f3a0-0000-7000-8000-00000000000a");
    private static final AdminProperties PROPERTIES =
            new AdminProperties(
                    new AdminProperties.UserExport(500), new AdminProperties.Restrictions(20));

    @Mock private UserRepository userRepository;
    @Mock private AdminActionRepository adminActionRepository;

    private AdminUserExportService service;

    @BeforeEach
    void setUp() {
        service = new AdminUserExportService(userRepository, adminActionRepository, PROPERTIES);
    }

    @Test
    @DisplayName(
            "Xuất danh sách chọn kèm email: truy vấn tài khoản một lần, số dòng trong nhật ký khớp"
                    + " số dòng trong file, tên file theo thời điểm xuất")
    void selectedWithEmail_queriesOnceAndAuditMatchesFile() throws IOException {
        User first = user("an.bui");
        User second = user("viet.luu");
        when(userRepository.findAllByUsernameIn(any())).thenReturn(List.of(second, first));
        ExportUsersRequest request =
                new ExportUsersRequest(
                        ExportScope.SELECTED,
                        null,
                        List.of("an.bui", "khong.co", "viet.luu"),
                        true,
                        " Khảo sát ");

        UserExport export = service.prepare(request, ADMIN_ID, NOW);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(export, out);

        List<String> lines = out.toString(StandardCharsets.UTF_8).lines().toList();
        assertThat(export.fileName()).isEqualTo("users-20261020-0500.csv");
        assertThat(lines).hasSize(3);
        assertThat(lines.get(1)).startsWith("an.bui,");
        assertThat(lines.get(2)).startsWith("viet.luu,");
        ArgumentCaptor<AdminAction> saved = ArgumentCaptor.forClass(AdminAction.class);
        verify(adminActionRepository).save(saved.capture());
        assertThat(saved.getValue().getDetails()).isEqualTo("scope=SELECTED;rows=2");
        assertThat(saved.getValue().getReason()).isEqualTo("Khảo sát");
        verify(userRepository).findAllByUsernameIn(any());
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("Ghi file lỗi giữa chừng: ghi log admin_users_export_failed rồi ném lại lỗi")
    void write_outputFails_logsAndRethrows(CapturedOutput output) {
        when(userRepository.findAllByUsernameIn(any())).thenReturn(List.of(user("an.bui")));
        UserExport export =
                service.prepare(
                        new ExportUsersRequest(
                                ExportScope.SELECTED, null, List.of("an.bui"), false, null),
                        ADMIN_ID,
                        NOW);
        OutputStream broken =
                new OutputStream() {
                    @Override
                    public void write(int value) throws IOException {
                        throw new IOException("client went away");
                    }
                };

        assertThatThrownBy(() -> service.write(export, broken)).isInstanceOf(IOException.class);
        assertThat(output).contains("admin_users_export_failed scope=SELECTED");
    }

    private static User user(String username) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(user, "createdAt", NOW.minusSeconds(60));
        user.setUsername(username);
        user.setEmail(username + "@gmail.com");
        user.setRole(Role.LEARNER);
        return user;
    }
}
