package com.sqljudge.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sqljudge.auth.dto.request.RegisterRequest;
import com.sqljudge.auth.dto.response.RegisterResponse;
import com.sqljudge.auth.entity.SignupProvider;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.auth.service.EmailVerificationService;
import com.sqljudge.auth.service.RegistrationService;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.ConflictException;
import com.sqljudge.common.security.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailVerificationService emailVerificationService;

    @InjectMocks private RegistrationService registrationService;

    @Test
    @DisplayName("Đăng ký hợp lệ: tạo LEARNER chưa xác minh, email chữ thường, mật khẩu đã băm")
    void register_validRequest_createsUnverifiedLearner() {
        when(passwordEncoder.encode("matkhau-manh-2026")).thenReturn("hashed");
        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RegisterResponse response =
                registrationService.register(request("minh.anh", "  Minh.Anh@Gmail.com "));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(saved.capture());
        User user = saved.getValue();
        assertThat(user.getUsername()).isEqualTo("minh.anh");
        assertThat(user.getEmail()).isEqualTo("minh.anh@gmail.com");
        assertThat(user.getPasswordHash()).isEqualTo("hashed");
        assertThat(user.getRole()).isEqualTo(Role.LEARNER);
        assertThat(user.getSignupProvider()).isEqualTo(SignupProvider.EMAIL);
        assertThat(user.getEmailVerifiedAt()).isNull();
        assertThat(response.email()).isEqualTo("minh.anh@gmail.com");
        verify(emailVerificationService).sendVerificationEmail(user);
    }

    @Test
    @DisplayName("Username đã có thì 409 USERNAME_TAKEN, không lưu, không gửi mail")
    void register_takenUsername_throwsConflict() {
        when(userRepository.existsByUsername("minh.anh")).thenReturn(true);

        assertThatThrownBy(() -> registrationService.register(request("minh.anh", "a@b.dev")))
                .isInstanceOf(ConflictException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.USERNAME_TAKEN);
        verify(userRepository, never()).saveAndFlush(any());
        verifyNoInteractions(emailVerificationService);
    }

    @Test
    @DisplayName("Email đã có (so sau khi chuẩn hoá) thì 409 EMAIL_TAKEN")
    void register_takenEmail_throwsConflict() {
        when(userRepository.existsByEmail("minh.anh@gmail.com")).thenReturn(true);

        assertThatThrownBy(
                        () ->
                                registrationService.register(
                                        request("minh.anh", "MINH.ANH@gmail.com")))
                .isInstanceOf(ConflictException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.EMAIL_TAKEN);
        verify(userRepository, never()).saveAndFlush(any());
    }

    private static RegisterRequest request(String username, String email) {
        return new RegisterRequest(username, email, "matkhau-manh-2026", true);
    }
}
