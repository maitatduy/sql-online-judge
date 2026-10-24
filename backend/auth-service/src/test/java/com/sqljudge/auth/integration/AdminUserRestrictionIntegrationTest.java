package com.sqljudge.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.sqljudge.auth.entity.AdminAction;
import com.sqljudge.auth.entity.AdminActionType;
import com.sqljudge.auth.entity.SignupProvider;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.entity.UserRestriction;
import com.sqljudge.auth.repository.AdminActionRepository;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.auth.repository.UserRestrictionRepository;
import com.sqljudge.common.security.Role;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AdminUserRestrictionIntegrationTest extends ContainerSupport {

    private static final String PASSWORD = "matkhau-manh-2026";

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private UserRestrictionRepository userRestrictionRepository;
    @Autowired private AdminActionRepository adminActionRepository;

    @Test
    @DisplayName(
            "Hạn chế: mặc định khi chưa đặt, sửa từng phần qua hai lần PATCH vẫn giữ trường còn"
                    + " lại, nhật ký có giá trị trước và sau")
    void restrictions_endToEnd() throws Exception {
        String adminToken = signInAsAdmin("admin.hanche", "admin.hanche@gmail.com");
        User target = saveUser("hanche.mot");
        String path = "/api/admin/users/hanche.mot/restrictions";

        ResponseEntity<Map> initial = exchange(adminToken, HttpMethod.GET, path, null);
        ResponseEntity<Map> quota =
                exchange(
                        adminToken,
                        HttpMethod.PATCH,
                        path,
                        Map.of("aiDailyQuota", 5, "reason", "Lạm dụng AI"));
        long versionAfterFirst = versionOf(target);
        ResponseEntity<Map> ratedBan =
                exchange(
                        adminToken,
                        HttpMethod.PATCH,
                        path,
                        Map.of("isBannedFromRatedContests", true, "reason", "Gian lận contest"));
        long versionAfterSecond = versionOf(target);
        ResponseEntity<Map> current = exchange(adminToken, HttpMethod.GET, path, null);

        assertThat(versionAfterSecond).isEqualTo(versionAfterFirst + 1);

        assertThat(initial.getBody())
                .containsEntry("aiDailyQuota", 20)
                .containsEntry("isBannedFromRatedContests", false);
        assertThat(quota.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ratedBan.getBody())
                .containsEntry("aiDailyQuota", 5)
                .containsEntry("isBannedFromRatedContests", true);
        assertThat(current.getBody()).isEqualTo(ratedBan.getBody());
        assertThat(userRestrictionRepository.findByUserId(target.getId())).isPresent();
        List<AdminAction> actions =
                adminActionRepository.findAll().stream()
                        .filter(action -> target.getId().equals(action.getTargetUserId()))
                        .toList();
        assertThat(actions)
                .extracting(
                        AdminAction::getAction,
                        AdminAction::getPreviousValue,
                        AdminAction::getNewValue,
                        AdminAction::getReason)
                .containsExactly(
                        tuple(
                                AdminActionType.UPDATE_RESTRICTIONS,
                                "aiDailyQuota=default(20);isBannedFromRatedContests=false",
                                "aiDailyQuota=5;isBannedFromRatedContests=false",
                                "Lạm dụng AI"),
                        tuple(
                                AdminActionType.UPDATE_RESTRICTIONS,
                                "aiDailyQuota=5;isBannedFromRatedContests=false",
                                "aiDailyQuota=5;isBannedFromRatedContests=true",
                                "Gian lận contest"));
    }

    @Test
    @DisplayName(
            "Hạn chế: tự đặt cho chính mình bị 422, tài khoản đã xoá vẫn xem được nhưng sửa bị 404")
    void restrictions_selfAndDeleted() throws Exception {
        String adminToken = signInAsAdmin("admin.tuhan", "admin.tuhan@gmail.com");
        User deleted = saveUser("hanche.xoa");
        deleted.markDeleted(Instant.now());
        userRepository.save(deleted);

        ResponseEntity<Map> self =
                exchange(
                        adminToken,
                        HttpMethod.PATCH,
                        "/api/admin/users/admin.tuhan/restrictions",
                        Map.of("aiDailyQuota", 0, "reason", "Thử"));
        ResponseEntity<Map> deletedView =
                exchange(
                        adminToken,
                        HttpMethod.GET,
                        "/api/admin/users/hanche.xoa/restrictions",
                        null);
        ResponseEntity<Map> deletedUpdate =
                exchange(
                        adminToken,
                        HttpMethod.PATCH,
                        "/api/admin/users/hanche.xoa/restrictions",
                        Map.of("aiDailyQuota", 0, "reason", "Thử"));

        assertThat(self.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(self.getBody()).containsEntry("code", "CANNOT_MODIFY_SELF");
        assertThat(deletedView.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(deletedUpdate.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(deletedUpdate.getBody()).containsEntry("code", "USER_NOT_FOUND");
    }

    @Test
    @DisplayName(
            "Hai admin cùng đặt hạn chế lần đầu cho một tài khoản cùng lúc: cả hai nhận 200, chỉ có"
                    + " một dòng hạn chế, không có lỗi 500")
    void restrictions_concurrentFirstWrites_createSingleRow() throws Exception {
        String adminToken = signInAsAdmin("admin.songsong", "admin.songsong@gmail.com");
        User target = saveUser("hanche.songsong");
        String path = "/api/admin/users/hanche.songsong/restrictions";
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        List<Future<ResponseEntity<Map>>> responses =
                List.of(
                        pool.submit(
                                () -> {
                                    start.await();
                                    return exchange(
                                            adminToken,
                                            HttpMethod.PATCH,
                                            path,
                                            Map.of("aiDailyQuota", 3, "reason", "Admin một"));
                                }),
                        pool.submit(
                                () -> {
                                    start.await();
                                    return exchange(
                                            adminToken,
                                            HttpMethod.PATCH,
                                            path,
                                            Map.of(
                                                    "isBannedFromRatedContests",
                                                    true,
                                                    "reason",
                                                    "Admin hai"));
                                }));
        start.countDown();
        List<HttpStatusCode> statuses = new ArrayList<>();
        for (Future<ResponseEntity<Map>> response : responses) {
            statuses.add(response.get(30, TimeUnit.SECONDS).getStatusCode());
        }
        pool.shutdown();

        assertThat(statuses).containsOnly(HttpStatus.OK);
        assertThat(userRestrictionRepository.findAll())
                .filteredOn(restriction -> restriction.getUserId().equals(target.getId()))
                .hasSize(1)
                .first()
                .satisfies(
                        restriction -> {
                            assertThat(restriction.getAiDailyQuota()).isEqualTo(3);
                            assertThat(restriction.isBannedFromRatedContests()).isTrue();
                        });
    }

    @Test
    @DisplayName(
            "Sửa bằng bản hạn chế cũ sau khi admin khác đã sửa (version đã tăng): bị từ chối bằng"
                    + " lỗi khoá lạc quan")
    void restrictions_staleCopy_isRejectedByOptimisticLock() throws Exception {
        String adminToken = signInAsAdmin("admin.lacquan", "admin.lacquan@gmail.com");
        User target = saveUser("hanche.lacquan");
        String path = "/api/admin/users/hanche.lacquan/restrictions";
        exchange(adminToken, HttpMethod.PATCH, path, Map.of("aiDailyQuota", 3, "reason", "Một"));
        UserRestriction stale =
                userRestrictionRepository.findByUserId(target.getId()).orElseThrow();

        exchange(adminToken, HttpMethod.PATCH, path, Map.of("aiDailyQuota", 4, "reason", "Hai"));
        stale.changeTo(9, null, 20);

        assertThat(versionOf(target)).isEqualTo(stale.getVersion() + 1);
        assertThatThrownBy(() -> userRestrictionRepository.save(stale))
                .isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(userRestrictionRepository.findByUserId(target.getId()).orElseThrow())
                .extracting(UserRestriction::getAiDailyQuota)
                .isEqualTo(4);
    }

    private long versionOf(User target) {
        return userRestrictionRepository.findByUserId(target.getId()).orElseThrow().getVersion();
    }

    private User saveUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@gmail.com");
        user.setRole(Role.LEARNER);
        user.setSignupProvider(SignupProvider.EMAIL);
        user.setEmailVerifiedAt(Instant.now());
        return userRepository.save(user);
    }

    private ResponseEntity<Map> exchange(
            String adminToken, HttpMethod method, String path, Map<String, ?> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        return restTemplate.exchange(path, method, new HttpEntity<>(body, headers), Map.class);
    }

    private String signInAsAdmin(String username, String email) throws Exception {
        restTemplate.postForEntity(
                "/api/auth/register",
                Map.of(
                        "username", username,
                        "email", email,
                        "password", PASSWORD,
                        "acceptedTerms", true),
                Map.class);
        String code =
                MailpitContainer.firstGroup(
                        MailpitContainer.CODE, MailpitContainer.client().awaitMailTo(email).text());
        restTemplate.postForEntity(
                "/api/auth/email-verification/confirm",
                Map.of("email", email, "code", code),
                Map.class);
        User admin = userRepository.findByUsername(username).orElseThrow();
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        return (String)
                restTemplate
                        .postForEntity(
                                "/api/auth/login",
                                Map.of("email", email, "password", PASSWORD, "rememberMe", false),
                                Map.class)
                        .getBody()
                        .get("accessToken");
    }
}
