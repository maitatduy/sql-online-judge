package com.sqljudge.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sqljudge.auth.dto.request.AddUserNoteRequest;
import com.sqljudge.auth.dto.response.UserNoteResponse;
import com.sqljudge.auth.dto.response.UserNotesResponse;
import com.sqljudge.auth.entity.AdminUserNote;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.repository.AdminUserNoteRepository;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.auth.service.AdminUserNoteService;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.AppException;
import com.sqljudge.common.security.CurrentUser;
import com.sqljudge.common.security.Role;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
import org.springframework.data.domain.Limit;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class AdminUserNoteServiceTest {

    private static final UUID ADMIN_ID = UUID.fromString("0192f3a0-0000-7000-8000-00000000000a");
    private static final UUID TARGET_ID = UUID.fromString("0192f3a0-0000-7000-8000-00000000000b");
    private static final UUID NOTE_ID = UUID.fromString("0192f3a0-0000-7000-8000-0000000000c1");
    private static final Instant CREATED_AT = Instant.parse("2026-10-21T03:00:00Z");
    private static final CurrentUser ADMIN =
            new CurrentUser(ADMIN_ID, "admin.thu", Set.of(Role.ADMIN));

    @Mock private UserRepository userRepository;
    @Mock private AdminUserNoteRepository adminUserNoteRepository;

    private AdminUserNoteService service;
    private User target;

    @BeforeEach
    void setUp() {
        service = new AdminUserNoteService(userRepository, adminUserNoteRepository);
        target = new User();
        ReflectionTestUtils.setField(target, "id", TARGET_ID);
        target.setUsername("viet.luu");
    }

    @Test
    @DisplayName(
            "Xem ghi chú: lấy tối đa 200 ghi chú mới nhất bằng một truy vấn, kể cả tài khoản đã"
                    + " xoá")
    void list_deletedUser_returnsLatestNotes() {
        target.markDeleted(CREATED_AT);
        UserNoteResponse note = new UserNoteResponse(NOTE_ID, "mod.thu", "Đã nhắc nhở", CREATED_AT);
        when(userRepository.findByUsername("viet.luu")).thenReturn(Optional.of(target));
        when(adminUserNoteRepository.findLatestByUserId(TARGET_ID, Limit.of(200)))
                .thenReturn(List.of(note));

        UserNotesResponse response = service.list("viet.luu");

        assertThat(response.items()).containsExactly(note);
    }

    @Test
    @DisplayName("Xem ghi chú của username không có: 404 USER_NOT_FOUND")
    void list_missingUser_throwsNotFound() {
        when(userRepository.findByUsername("khong.co")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.list("khong.co"))
                .isInstanceOfSatisfying(
                        AppException.class,
                        rejected ->
                                assertThat(rejected.getCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
    }

    @Test
    @DisplayName(
            "Thêm ghi chú: lưu nội dung, người viết lấy từ JWT, trả tên người viết; log chỉ có id,"
                    + " không có nội dung")
    void add_liveUser_savesNoteWithoutLoggingBody(CapturedOutput output) {
        when(userRepository.findByUsername("viet.luu")).thenReturn(Optional.of(target));
        when(adminUserNoteRepository.save(any(AdminUserNote.class)))
                .thenAnswer(
                        invocation -> {
                            AdminUserNote saved = invocation.getArgument(0);
                            ReflectionTestUtils.setField(saved, "id", NOTE_ID);
                            ReflectionTestUtils.setField(saved, "createdAt", CREATED_AT);
                            return saved;
                        });

        UserNoteResponse response =
                service.add("viet.luu", new AddUserNoteRequest("Bí mật nội bộ"), ADMIN);

        assertThat(response)
                .isEqualTo(new UserNoteResponse(NOTE_ID, "admin.thu", "Bí mật nội bộ", CREATED_AT));
        ArgumentCaptor<AdminUserNote> saved = ArgumentCaptor.forClass(AdminUserNote.class);
        verify(adminUserNoteRepository).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(TARGET_ID);
        assertThat(saved.getValue().getAuthorId()).isEqualTo(ADMIN_ID);
        assertThat(output).contains("admin_user_note_added noteId=" + NOTE_ID);
        assertThat(output).doesNotContain("Bí mật nội bộ");
    }

    @Test
    @DisplayName("Thêm ghi chú cho tài khoản đã xoá: 404, không lưu gì")
    void add_deletedUser_throwsNotFound() {
        target.markDeleted(CREATED_AT);
        when(userRepository.findByUsername(eq("viet.luu"))).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.add("viet.luu", new AddUserNoteRequest("Ghi chú"), ADMIN))
                .isInstanceOfSatisfying(
                        AppException.class,
                        rejected ->
                                assertThat(rejected.getCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
        verifyNoInteractions(adminUserNoteRepository);
    }

    @Test
    @DisplayName("Nội dung ghi chú được bỏ khoảng trắng hai đầu trước khi kiểm tra và lưu")
    void request_stripsBody() {
        assertThat(new AddUserNoteRequest("  Ghi chú  ").body()).isEqualTo("Ghi chú");
        assertThat(new AddUserNoteRequest(null).body()).isNull();
    }
}
