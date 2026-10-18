package com.sqljudge.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.auth.service.UsernameGenerator;
import com.sqljudge.auth.validation.UsernameRules;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UsernameGeneratorTest {

    @Mock private UserRepository userRepository;

    @InjectMocks private UsernameGenerator usernameGenerator;

    @ParameterizedTest
    @CsvSource({
        "Minh-Anh, minh_anh",
        "minh.anh, minh.anh",
        "Nguyễn Văn A, nguy_n_v_n_a",
        "ab, ab_",
        "'', user",
        "me, me_"
    })
    @DisplayName("Chuẩn hoá tên từ nhà cung cấp thành username hợp lệ")
    void generate_normalizesToValidUsername(String source, String expected) {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);

        String username = usernameGenerator.generate(source);

        assertThat(username).isEqualTo(expected).matches(UsernameRules.PATTERN);
    }

    @Test
    @DisplayName("Tên quá dài bị cắt còn 30 ký tự")
    void generate_longName_isTruncated() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);

        assertThat(usernameGenerator.generate("a".repeat(50))).hasSize(30);
    }

    @Test
    @DisplayName("Username đã có người dùng thì thêm hậu tố số cho tới khi trống")
    void generate_takenUsername_appendsNumericSuffix() {
        when(userRepository.existsByUsername("minh_anh")).thenReturn(true);
        when(userRepository.existsByUsername("minh_anh1")).thenReturn(true);
        when(userRepository.existsByUsername("minh_anh2")).thenReturn(false);

        assertThat(usernameGenerator.generate("Minh-Anh")).isEqualTo("minh_anh2");
    }
}
