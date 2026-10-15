package com.sqljudge.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sqljudge.auth.entity.SignupProvider;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.common.config.CommonAutoConfiguration;
import com.sqljudge.common.security.Role;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(CommonAutoConfiguration.class)
class UserRepositoryTest extends MySqlContainerSupport {

    @Autowired private UserRepository userRepository;

    @Test
    @DisplayName("Lưu user qua migration thật: id UUIDv7, cột audit và version được điền")
    void save_newUser_generatesIdAndAuditColumns() {
        User saved = userRepository.saveAndFlush(learner("minh.anh", "minh.anh@sqljudge.dev"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getId().version()).isEqualTo(7);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getVersion()).isZero();
    }

    @Test
    @DisplayName("existsByUsername tính cả tài khoản đã xoá mềm")
    void existsByUsername_includesSoftDeletedUsers() {
        User deleted = learner("da.xoa", "da.xoa@sqljudge.dev");
        deleted.setDeletedAt(Instant.parse("2026-10-01T00:00:00Z"));
        userRepository.saveAndFlush(deleted);

        assertThat(userRepository.existsByUsername("da.xoa")).isTrue();
        assertThat(userRepository.existsByUsername("chua.co")).isFalse();
    }

    @Test
    @DisplayName("Trùng username thì DB chặn bằng uk_users_username")
    void save_duplicateUsername_isRejectedByUniqueIndex() {
        userRepository.saveAndFlush(learner("trung.ten", "a@sqljudge.dev"));

        assertThatThrownBy(
                        () -> userRepository.saveAndFlush(learner("trung.ten", "b@sqljudge.dev")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Trùng email khác hoa thường thì DB vẫn chặn bằng uk_users_email")
    void save_duplicateEmailIgnoringCase_isRejectedByUniqueIndex() {
        userRepository.saveAndFlush(learner("nguoi.mot", "trung@sqljudge.dev"));

        assertThatThrownBy(
                        () ->
                                userRepository.saveAndFlush(
                                        learner("nguoi.hai", "Trung@SQLJudge.dev")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static User learner(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setRole(Role.LEARNER);
        user.setSignupProvider(SignupProvider.EMAIL);
        return user;
    }
}
