package com.sqljudge.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.auth.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;

    @InjectMocks private UserService userService;

    @Test
    @DisplayName("Username chưa ai dùng thì còn trống")
    void checkUsernameAvailability_unusedUsername_isAvailable() {
        when(userRepository.existsByUsername("minh.anh")).thenReturn(false);

        assertThat(userService.checkUsernameAvailability("minh.anh").available()).isTrue();
    }

    @Test
    @DisplayName("Username đã có người dùng thì không còn trống")
    void checkUsernameAvailability_takenUsername_isUnavailable() {
        when(userRepository.existsByUsername("minh.anh")).thenReturn(true);

        assertThat(userService.checkUsernameAvailability("minh.anh").available()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"me", "availability"})
    @DisplayName("Tên dành riêng không bao giờ còn trống và không cần hỏi DB")
    void checkUsernameAvailability_reservedUsername_isUnavailable(String reserved) {
        assertThat(userService.checkUsernameAvailability(reserved).available()).isFalse();
        verifyNoInteractions(userRepository);
    }
}
