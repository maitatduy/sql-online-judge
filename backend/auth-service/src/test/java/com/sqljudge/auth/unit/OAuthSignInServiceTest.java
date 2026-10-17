package com.sqljudge.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sqljudge.auth.entity.OAuthAccount;
import com.sqljudge.auth.entity.SignupProvider;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.repository.OAuthAccountRepository;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.auth.service.OAuthProfile;
import com.sqljudge.auth.service.OAuthSignInService;
import com.sqljudge.auth.service.SignInResult;
import com.sqljudge.auth.service.SignInService;
import com.sqljudge.auth.service.UsernameGenerator;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.AppException;
import com.sqljudge.common.security.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OAuthSignInServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-17T05:00:00Z");
    private static final String USER_AGENT = "Mozilla/5.0";
    private static final OAuthProfile VERIFIED_PROFILE =
            new OAuthProfile(
                    SignupProvider.GITHUB,
                    "583231",
                    "Minh.Anh@Gmail.com",
                    true,
                    "Minh-Anh",
                    "Minh Anh");

    @Mock private OAuthAccountRepository oauthAccountRepository;
    @Mock private UserRepository userRepository;
    @Mock private UsernameGenerator usernameGenerator;
    @Mock private SignInService signInService;
    @Mock private SignInResult signInResult;

    private OAuthSignInService service;
    private User existingUser;

    @BeforeEach
    void setUp() {
        service =
                new OAuthSignInService(
                        oauthAccountRepository,
                        userRepository,
                        usernameGenerator,
                        signInService,
                        Clock.fixed(NOW, ZoneOffset.UTC));
        existingUser = new User();
        ReflectionTestUtils.setField(existingUser, "id", UUID.randomUUID());
        existingUser.setEmail("minh.anh@gmail.com");
        existingUser.setEmailVerifiedAt(NOW.minusSeconds(3600));
    }

    @Test
    @DisplayName("Tài khoản GitHub đã liên kết: đăng nhập luôn vào user đó, phiên ghi nhớ")
    void signIn_linkedAccount_signsInLinkedUser() {
        OAuthAccount account = new OAuthAccount();
        account.setUser(existingUser);
        when(oauthAccountRepository.findByProviderAndProviderUserId(
                        SignupProvider.GITHUB, "583231"))
                .thenReturn(Optional.of(account));
        when(signInService.signIn(existingUser.getId(), true, USER_AGENT)).thenReturn(signInResult);

        assertThat(service.signIn(VERIFIED_PROFILE, USER_AGENT)).isSameAs(signInResult);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName(
            "Chưa liên kết nhưng trùng email đã verified: liên kết vào tài khoản có sẵn và xác thực"
                    + " email")
    void signIn_matchingVerifiedEmail_linksExistingUser() {
        existingUser.setEmailVerifiedAt(null);
        when(oauthAccountRepository.findByProviderAndProviderUserId(
                        SignupProvider.GITHUB, "583231"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("minh.anh@gmail.com"))
                .thenReturn(Optional.of(existingUser));
        when(signInService.signIn(existingUser.getId(), true, USER_AGENT)).thenReturn(signInResult);

        service.signIn(VERIFIED_PROFILE, USER_AGENT);

        OAuthAccount linked = savedAccount();
        assertThat(linked.getUser()).isSameAs(existingUser);
        assertThat(linked.getProvider()).isEqualTo(SignupProvider.GITHUB);
        assertThat(linked.getProviderUserId()).isEqualTo("583231");
        assertThat(existingUser.getEmailVerifiedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName(
            "Chưa có tài khoản: tạo LEARNER đã xác thực, không mật khẩu, username sinh từ GitHub")
    void signIn_newEmail_createsVerifiedLearner() {
        when(oauthAccountRepository.findByProviderAndProviderUserId(
                        SignupProvider.GITHUB, "583231"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("minh.anh@gmail.com")).thenReturn(Optional.empty());
        when(usernameGenerator.generate("Minh-Anh")).thenReturn("minh_anh");
        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(
                        invocation -> {
                            User saved = invocation.getArgument(0);
                            ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
                            return saved;
                        });
        when(signInService.signIn(any(UUID.class), anyBoolean(), anyString()))
                .thenReturn(signInResult);

        service.signIn(VERIFIED_PROFILE, USER_AGENT);

        ArgumentCaptor<User> created = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(created.capture());
        User user = created.getValue();
        assertThat(user.getUsername()).isEqualTo("minh_anh");
        assertThat(user.getEmail()).isEqualTo("minh.anh@gmail.com");
        assertThat(user.getDisplayName()).isEqualTo("Minh Anh");
        assertThat(user.getRole()).isEqualTo(Role.LEARNER);
        assertThat(user.getSignupProvider()).isEqualTo(SignupProvider.GITHUB);
        assertThat(user.getPasswordHash()).isNull();
        assertThat(user.getEmailVerifiedAt()).isEqualTo(NOW);
        assertThat(savedAccount().getUser()).isSameAs(user);
    }

    @Test
    @DisplayName(
            "GitHub không có email đã verified: từ chối OAUTH_EMAIL_NOT_VERIFIED, không tạo gì")
    void signIn_unverifiedEmail_rejects() {
        OAuthProfile unverified =
                new OAuthProfile(SignupProvider.GITHUB, "583231", null, false, "minh", null);
        when(oauthAccountRepository.findByProviderAndProviderUserId(
                        SignupProvider.GITHUB, "583231"))
                .thenReturn(Optional.empty());

        assertRejected(unverified, ErrorCode.OAUTH_EMAIL_NOT_VERIFIED);
        verify(userRepository, never()).saveAndFlush(any());
        verify(oauthAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tài khoản liên kết đang bị khoá: ACCOUNT_BANNED; đã xoá: OAUTH_LOGIN_FAILED")
    void signIn_bannedOrDeletedUser_rejects() {
        OAuthAccount account = new OAuthAccount();
        account.setUser(existingUser);
        when(oauthAccountRepository.findByProviderAndProviderUserId(
                        SignupProvider.GITHUB, "583231"))
                .thenReturn(Optional.of(account));

        existingUser.setBannedAt(NOW.minusSeconds(60));
        assertRejected(VERIFIED_PROFILE, ErrorCode.ACCOUNT_BANNED);

        existingUser.setBannedAt(null);
        existingUser.setDeletedAt(NOW.minusSeconds(60));
        assertRejected(VERIFIED_PROFILE, ErrorCode.OAUTH_LOGIN_FAILED);
    }

    private OAuthAccount savedAccount() {
        ArgumentCaptor<OAuthAccount> captor = ArgumentCaptor.forClass(OAuthAccount.class);
        verify(oauthAccountRepository).save(captor.capture());
        return captor.getValue();
    }

    private void assertRejected(OAuthProfile profile, ErrorCode code) {
        assertThatThrownBy(() -> service.signIn(profile, USER_AGENT))
                .isInstanceOf(AppException.class)
                .extracting("code")
                .isEqualTo(code);
        verify(signInService, never()).signIn(any(), anyBoolean(), anyString());
    }
}
