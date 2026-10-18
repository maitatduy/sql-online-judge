package com.sqljudge.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sqljudge.auth.dto.request.CreateUserRequest;
import com.sqljudge.auth.dto.response.AccountStatus;
import com.sqljudge.auth.dto.response.AdminUserDetailResponse;
import com.sqljudge.auth.entity.EmailToken;
import com.sqljudge.auth.entity.EmailTokenPurpose;
import com.sqljudge.auth.entity.SignupProvider;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.event.InvitationEmailRequestedEvent;
import com.sqljudge.auth.repository.EmailTokenRepository;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.auth.service.AdminUserCommandService;
import com.sqljudge.auth.service.SecureTokens;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.ConflictException;
import com.sqljudge.common.security.Role;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class AdminUserCommandServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-18T05:00:00Z");
    private static final CreateUserRequest REQUEST =
            new CreateUserRequest("lan.pham", " Lan.Pham@Gmail.com ", Role.SETTER, " Phạm Lan ");

    @Mock private UserRepository userRepository;
    @Mock private EmailTokenRepository emailTokenRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private AdminUserCommandService service;

    @BeforeEach
    void setUp() {
        service =
                new AdminUserCommandService(
                        userRepository,
                        emailTokenRepository,
                        eventPublisher,
                        Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName(
            "Tạo tài khoản chưa có mật khẩu, chưa xác minh, email chuẩn hoá, tên hiển thị bỏ khoảng"
                    + " trắng")
    void createUser_newAccount_savesUnverifiedAccountWithoutPassword() {
        stubFreeUsernameAndEmail();
        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminUserDetailResponse created = service.createUser(REQUEST);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getUsername()).isEqualTo("lan.pham");
        assertThat(saved.getValue().getEmail()).isEqualTo("lan.pham@gmail.com");
        assertThat(saved.getValue().getRole()).isEqualTo(Role.SETTER);
        assertThat(saved.getValue().getDisplayName()).isEqualTo("Phạm Lan");
        assertThat(saved.getValue().getSignupProvider()).isEqualTo(SignupProvider.EMAIL);
        assertThat(saved.getValue().getPasswordHash()).isNull();
        assertThat(saved.getValue().isEmailVerified()).isFalse();
        assertThat(created.account().status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(created.account().isEmailVerified()).isFalse();
        assertThat(created.displayName()).isEqualTo("Phạm Lan");
        assertThat(created.violations()).isEmpty();
    }

    @Test
    @DisplayName("Tên hiển thị trống được lưu là null")
    void createUser_blankDisplayName_storesNull() {
        stubFreeUsernameAndEmail();
        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminUserDetailResponse created =
                service.createUser(
                        new CreateUserRequest(
                                "lan.pham", "lan.pham@gmail.com", Role.LEARNER, "   "));

        assertThat(created.displayName()).isNull();
    }

    @Test
    @DisplayName("Gửi link mời sống 7 ngày, chỉ lưu hash của link")
    void createUser_newAccount_issuesSevenDayInvite() {
        stubFreeUsernameAndEmail();
        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.createUser(REQUEST);

        ArgumentCaptor<EmailToken> invite = ArgumentCaptor.forClass(EmailToken.class);
        verify(emailTokenRepository).save(invite.capture());
        ArgumentCaptor<InvitationEmailRequestedEvent> event =
                ArgumentCaptor.forClass(InvitationEmailRequestedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(invite.getValue().getPurpose()).isEqualTo(EmailTokenPurpose.INVITE);
        assertThat(invite.getValue().getExpiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
        assertThat(invite.getValue().getCodeHash()).isNull();
        assertThat(invite.getValue().getTokenHash())
                .isEqualTo(SecureTokens.sha256(event.getValue().token()));
        assertThat(event.getValue().email()).isEqualTo("lan.pham@gmail.com");
        assertThat(event.getValue().username()).isEqualTo("lan.pham");
    }

    @Test
    @DisplayName("Username đã có người dùng: 409 USERNAME_TAKEN, không lưu, không gửi mail")
    void createUser_takenUsername_rejects() {
        when(userRepository.existsByUsername("lan.pham")).thenReturn(true);

        assertThatThrownBy(() -> service.createUser(REQUEST))
                .isInstanceOf(ConflictException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.USERNAME_TAKEN);
        verify(userRepository, never()).saveAndFlush(any());
        verifyNoInteractions(emailTokenRepository, eventPublisher);
    }

    @Test
    @DisplayName("Email đã được dùng (so sau khi chuẩn hoá): 409 EMAIL_TAKEN")
    void createUser_takenEmail_rejects() {
        when(userRepository.existsByUsername("lan.pham")).thenReturn(false);
        when(userRepository.existsByEmail("lan.pham@gmail.com")).thenReturn(true);

        assertThatThrownBy(() -> service.createUser(REQUEST))
                .isInstanceOf(ConflictException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.EMAIL_TAKEN);
        verify(userRepository, never()).saveAndFlush(any());
        verifyNoInteractions(emailTokenRepository, eventPublisher);
    }

    private void stubFreeUsernameAndEmail() {
        when(userRepository.existsByUsername("lan.pham")).thenReturn(false);
        when(userRepository.existsByEmail("lan.pham@gmail.com")).thenReturn(false);
    }
}
