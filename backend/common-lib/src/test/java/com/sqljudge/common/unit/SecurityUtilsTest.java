package com.sqljudge.common.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sqljudge.common.persistence.SecurityAuditorAware;
import com.sqljudge.common.security.CurrentUser;
import com.sqljudge.common.security.Role;
import com.sqljudge.common.security.SecurityUtils;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityUtilsTest {

    private static final CurrentUser LEARNER =
            new CurrentUser(UUID.randomUUID(), "minh.nguyen", Set.of(Role.LEARNER));

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Có người dùng đã xác thực thì trả về đúng người dùng đó")
    void currentUser_authenticated_returnsPrincipal() {
        authenticate(LEARNER);

        assertThat(SecurityUtils.currentUser()).contains(LEARNER);
        assertThat(SecurityUtils.requireCurrentUser()).isEqualTo(LEARNER);
        assertThat(SecurityUtils.currentUserId()).contains(LEARNER.id());
    }

    @Test
    @DisplayName("Chưa đăng nhập thì không có người dùng hiện tại")
    void currentUser_noAuthentication_returnsEmpty() {
        assertThat(SecurityUtils.currentUser()).isEmpty();
        assertThatThrownBy(SecurityUtils::requireCurrentUser)
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    @DisplayName("Người dùng ẩn danh không được coi là người dùng hiện tại")
    void currentUser_anonymous_returnsEmpty() {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new AnonymousAuthenticationToken(
                                "key",
                                "anonymousUser",
                                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertThat(SecurityUtils.currentUser()).isEmpty();
    }

    @Test
    @DisplayName("Auditor là id của người dùng hiện tại, trống khi chưa đăng nhập")
    void auditorAware_followsCurrentUser() {
        SecurityAuditorAware auditorAware = new SecurityAuditorAware();
        assertThat(auditorAware.getCurrentAuditor()).isEmpty();

        authenticate(LEARNER);

        assertThat(auditorAware.getCurrentAuditor()).contains(LEARNER.id());
    }

    @Test
    @DisplayName("Vai trò ánh xạ sang authority ROLE_ và kiểm tra được trên người dùng")
    void role_authorityAndHasRole() {
        assertThat(Role.MODERATOR.authority()).isEqualTo("ROLE_MODERATOR");
        assertThat(LEARNER.hasRole(Role.LEARNER)).isTrue();
        assertThat(LEARNER.hasRole(Role.ADMIN)).isFalse();
    }

    private static void authenticate(CurrentUser user) {
        List<SimpleGrantedAuthority> authorities =
                user.roles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.authority()))
                        .toList();
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(user, null, authorities));
    }
}
