package com.sqljudge.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.auth.entity.SignupProvider;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.entity.UserRestriction;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.auth.repository.UserRestrictionRepository;
import com.sqljudge.common.security.Role;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
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

    @Test
    @DisplayName("Tra tên nhiều người dùng một lần, bỏ qua người không tồn tại hoặc đã xoá")
    void internalUsers_byIds_returnsNamesOfActiveUsers() {
        User first = saveUser("noibo.nguoira1");
        User second = saveUser("noibo.nguoira2");
        User deleted = saveUser("noibo.nguoixoa");
        deleted.setDeletedAt(Instant.now());
        userRepository.save(deleted);
        String ids =
                String.join(",", first.getId() + "", second.getId() + "", deleted.getId() + "");

        ResponseEntity<List<Map<String, Object>>> response =
                internalGetList("/internal/users?ids=" + ids + "," + UUID.randomUUID());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .extracting(user -> user.get("username"))
                .containsExactlyInAnyOrder("noibo.nguoira1", "noibo.nguoira2");
        assertThat(response.getBody().get(0)).doesNotContainKey("email");
    }

    @Test
    @DisplayName("Tra quá 100 người dùng một lần trả 400")
    void internalUsers_tooManyIds_isRejected() {
        String ids =
                String.join(
                        ",",
                        Stream.generate(() -> UUID.randomUUID().toString()).limit(101).toList());

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Token", INTERNAL_TOKEN);

        assertThat(
                        restTemplate
                                .exchange(
                                        "/internal/users?ids=" + ids,
                                        HttpMethod.GET,
                                        new HttpEntity<>(headers),
                                        String.class)
                                .getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<List<Map<String, Object>>> internalGetList(String path) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Token", INTERNAL_TOKEN);
        return restTemplate.exchange(
                path,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});
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
