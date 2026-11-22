package com.sqljudge.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.auth.entity.SignupProvider;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.entity.UserRestriction;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.auth.repository.UserRestrictionRepository;
import com.sqljudge.common.security.Role;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class InternalUserIntegrationTest extends ContainerSupport {

    private static final String INTERNAL_TOKEN = "test-internal-token";
    private static final int DEFAULT_AI_QUOTA = 20;

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private UserRestrictionRepository userRestrictionRepository;

    @Test
    @DisplayName("Trả tên hiển thị và cờ cấm thi contest tính rating của người dùng")
    void internalUser_returnsContestEligibility() {
        User allowed = saveUser("noibo.duocthi");
        User banned = saveUser("noibo.bicam");
        UserRestriction restriction = UserRestriction.defaultsFor(banned);
        restriction.changeTo(null, true, DEFAULT_AI_QUOTA);
        userRestrictionRepository.save(restriction);

        ResponseEntity<Map> allowedResponse = internalGet(allowed.getId(), INTERNAL_TOKEN);
        ResponseEntity<Map> bannedResponse = internalGet(banned.getId(), INTERNAL_TOKEN);

        assertThat(allowedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(allowedResponse.getBody())
                .containsEntry("userId", allowed.getId().toString())
                .containsEntry("username", "noibo.duocthi")
                .containsEntry("isBannedFromRatedContests", false)
                .doesNotContainKey("email");
        assertThat(bannedResponse.getBody()).containsEntry("isBannedFromRatedContests", true);
    }

    @Test
    @DisplayName("Người dùng không tồn tại hoặc đã xoá trả 404")
    void internalUser_unknownOrDeleted_returnsNotFound() {
        User deleted = saveUser("noibo.daxoa");
        deleted.setDeletedAt(Instant.now());
        userRepository.save(deleted);

        assertThat(internalGet(UUID.randomUUID(), INTERNAL_TOKEN).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(internalGet(deleted.getId(), INTERNAL_TOKEN).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Thiếu hoặc sai token nội bộ trả 401, JWKS vẫn công khai")
    void internalUser_withoutToken_isRejected() {
        User user = saveUser("noibo.khongtoken");

        assertThat(internalGet(user.getId(), null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(internalGet(user.getId(), "sai-token").getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(restTemplate.getForEntity("/internal/jwks", String.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    private ResponseEntity<Map> internalGet(UUID userId, String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.set("X-Internal-Token", token);
        }
        return restTemplate.exchange(
                "/internal/users/" + userId, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
    }

    private User saveUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setDisplayName("Người " + username);
        user.setEmail(username + "@gmail.com");
        user.setRole(Role.LEARNER);
        user.setSignupProvider(SignupProvider.EMAIL);
        user.setEmailVerifiedAt(Instant.now());
        return userRepository.save(user);
    }
}
