package com.sqljudge.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.auth.entity.AdminAction;
import com.sqljudge.auth.entity.AdminActionType;
import com.sqljudge.auth.entity.SignupProvider;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.repository.AdminActionRepository;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.common.security.Role;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AdminUserExportIntegrationTest extends ContainerSupport {

    private static final String PASSWORD = "matkhau-manh-2026";
    private static final String EXPORT_PATH = "/api/admin/user-exports";
    private static final Comparator<UUID> UNSIGNED_BYTE_ORDER =
            Comparator.<UUID>comparingLong(id -> id.getMostSignificantBits() ^ Long.MIN_VALUE)
                    .thenComparingLong(id -> id.getLeastSignificantBits() ^ Long.MIN_VALUE);

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private AdminActionRepository adminActionRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName(
            "Xuất theo bộ lọc kèm email: file CSV đúng tên, đúng dòng, chống CSV injection; nhật ký"
                    + " không gắn tài khoản nào và không chứa dữ liệu cá nhân")
    void exportFilteredWithEmail_endToEnd() throws Exception {
        String adminToken = signInAsAdmin("admin.xuat", "admin.xuat@gmail.com");
        saveUser("xuat.mot", "xuat.mot@gmail.com", Role.MODERATOR);
        saveUser("xuat.hai", "=cmd@gmail.com", Role.MODERATOR);
        saveUser("xuat.ba", "xuat.ba@gmail.com", Role.LEARNER);

        ResponseEntity<String> response =
                export(
                        adminToken,
                        Map.of(
                                "scope",
                                "FILTERED",
                                "filters",
                                Map.of("role", "MODERATOR", "q", "xuat"),
                                "includeEmail",
                                true,
                                "reason",
                                "Liên hệ kiểm duyệt viên"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType().toString())
                .isEqualTo("text/csv;charset=UTF-8");
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .matches("attachment; filename=\"users-\\d{8}-\\d{4}\\.csv\"");
        List<String> lines = Arrays.asList(response.getBody().split("\n"));
        assertThat(lines.get(0)).isEqualTo("username,email,role,status,joinedAt");
        assertThat(lines.subList(1, lines.size()))
                .extracting(line -> line.substring(0, line.indexOf(",MODERATOR")))
                .containsExactly("xuat.hai,'=cmd@gmail.com", "xuat.mot,xuat.mot@gmail.com");
        List<AdminAction> exports =
                adminActionRepository.findAll().stream()
                        .filter(action -> action.getAction() == AdminActionType.EXPORT_USER_EMAILS)
                        .filter(action -> "Liên hệ kiểm duyệt viên".equals(action.getReason()))
                        .toList();
        assertThat(exports).hasSize(1);
        AdminAction audit = exports.get(0);
        assertThat(audit.getTargetUserId()).isNull();
        assertThat(audit.getReason()).isEqualTo("Liên hệ kiểm duyệt viên");
        assertThat(audit.getDetails()).isEqualTo("scope=FILTERED;rows=2;q=*;role=MODERATOR");
        assertThat(userRepository.findAll())
                .allSatisfy(user -> assertThat(actionsTargeting(user)).doesNotContain(audit));
    }

    @Test
    @DisplayName(
            "Xuất danh sách chọn không kèm email: giữ thứ tự gửi lên, có cả tài khoản đã xoá, bỏ"
                    + " qua username không có, không ghi nhật ký")
    void exportSelectedWithoutEmail_keepsOrderAndSkipsUnknown() throws Exception {
        String adminToken = signInAsAdmin("admin.chon", "admin.chon@gmail.com");
        saveUser("chon.mot", "chon.mot@gmail.com", Role.LEARNER);
        User deleted = saveUser("chon.xoa", "chon.xoa@gmail.com", Role.SETTER);
        deleted.markDeleted(Instant.now());
        userRepository.save(deleted);
        long auditsBefore = adminActionRepository.count();

        ResponseEntity<String> response =
                export(
                        adminToken,
                        Map.of(
                                "scope",
                                "SELECTED",
                                "usernames",
                                List.of("chon.xoa", "khong.co", "chon.mot"),
                                "includeEmail",
                                false));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<String> lines = Arrays.asList(response.getBody().split("\n"));
        assertThat(lines.get(0)).isEqualTo("username,role,status,joinedAt");
        assertThat(lines.subList(1, lines.size()))
                .extracting(line -> line.substring(0, line.indexOf(',')))
                .containsExactly("chon.xoa", "chon.mot");
        assertThat(response.getBody()).doesNotContain("@gmail.com");
        assertThat(adminActionRepository.count()).isEqualTo(auditsBefore);
    }

    @Test
    @DisplayName(
            "Xuất theo lô nhỏ (2 dòng/lô) qua nhiều lô có tài khoản trùng thời điểm tạo: đủ dòng,"
                    + " không trùng, đúng thứ tự mới nhất trước")
    void exportFiltered_acrossBatchesWithTies_keepsEveryRowOnceInOrder() throws Exception {
        String adminToken = signInAsAdmin("admin.lo", "admin.lo@gmail.com");
        List<String> usernames = List.of("lo.mot", "lo.hai", "lo.ba", "lo.bon", "lo.nam");
        usernames.forEach(username -> saveUser(username, username + "@lo.test", Role.LEARNER));
        Instant tied = Instant.parse("2026-01-01T00:00:00Z");
        setCreatedAt(tied, "lo.mot", "lo.hai", "lo.ba");
        setCreatedAt(tied.minusSeconds(1), "lo.bon");
        setCreatedAt(tied.plusSeconds(1), "lo.nam");

        ResponseEntity<String> response =
                export(
                        adminToken,
                        Map.of(
                                "scope",
                                "FILTERED",
                                "filters",
                                Map.of("q", "@lo.test"),
                                "includeEmail",
                                false));

        List<String> lines = Arrays.asList(response.getBody().split("\n"));
        List<String> exported =
                lines.subList(1, lines.size()).stream()
                        .map(line -> line.substring(0, line.indexOf(',')))
                        .toList();
        List<String> newestFirst =
                usernames.stream()
                        .map(username -> userRepository.findByUsername(username).orElseThrow())
                        .sorted(
                                Comparator.comparing(User::getCreatedAt)
                                        .thenComparing(User::getId, UNSIGNED_BYTE_ORDER)
                                        .reversed())
                        .map(User::getUsername)
                        .toList();
        assertThat(exported).hasSize(5).doesNotHaveDuplicates().isEqualTo(newestFirst);
        assertThat(exported.get(0)).isEqualTo("lo.nam");
        assertThat(exported.get(4)).isEqualTo("lo.bon");
    }

    @Test
    @DisplayName("Xuất kèm email mà thiếu lý do: 400 trên trường reason")
    void exportWithEmailWithoutReason_isRejected() throws Exception {
        String adminToken = signInAsAdmin("admin.thieu", "admin.thieu@gmail.com");

        ResponseEntity<String> response =
                export(adminToken, Map.of("scope", "ALL", "includeEmail", true));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("\"field\":\"reason\"");
    }

    private void setCreatedAt(Instant createdAt, String... usernames) {
        for (String username : usernames) {
            jdbcTemplate.update(
                    "UPDATE users SET created_at = ? WHERE username = ?",
                    Timestamp.from(createdAt),
                    username);
        }
    }

    private List<AdminAction> actionsTargeting(User user) {
        return adminActionRepository.findAll().stream()
                .filter(action -> user.getId().equals(action.getTargetUserId()))
                .toList();
    }

    private ResponseEntity<String> export(String adminToken, Map<String, ?> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        return restTemplate.postForEntity(
                EXPORT_PATH, new HttpEntity<>(body, headers), String.class);
    }

    private User saveUser(String username, String email, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setRole(role);
        user.setSignupProvider(SignupProvider.EMAIL);
        user.setEmailVerifiedAt(Instant.now());
        return userRepository.save(user);
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
