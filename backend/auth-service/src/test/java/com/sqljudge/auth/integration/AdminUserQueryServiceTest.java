package com.sqljudge.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sqljudge.auth.dto.response.AccountStatus;
import com.sqljudge.auth.dto.response.AdminUserDetailResponse;
import com.sqljudge.auth.dto.response.AdminUserSummaryResponse;
import com.sqljudge.auth.dto.response.UserCountsResponse;
import com.sqljudge.auth.entity.SignupProvider;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.repository.EmailTokenRepository;
import com.sqljudge.auth.repository.OAuthAccountRepository;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.auth.repository.UserSessionRepository;
import com.sqljudge.auth.service.AdminUserQueryService;
import com.sqljudge.auth.service.AdminUserSearch;
import com.sqljudge.common.config.CommonAutoConfiguration;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.common.security.Role;
import com.sqljudge.common.web.PageResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(CommonAutoConfiguration.class)
@Import(AdminUserQueryService.class)
class AdminUserQueryServiceTest extends ContainerSupport {

    private static final Instant NOW = Instant.now();

    @Autowired private UserRepository userRepository;
    @Autowired private EmailTokenRepository emailTokenRepository;
    @Autowired private UserSessionRepository userSessionRepository;
    @Autowired private OAuthAccountRepository oauthAccountRepository;
    @Autowired private AdminUserQueryService service;

    @BeforeEach
    void seedUsers() {
        emailTokenRepository.deleteAllInBatch();
        userSessionRepository.deleteAllInBatch();
        oauthAccountRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        save(user("lan.pham", "lan.pham@gmail.com", Role.SETTER));
        User unverified = user("minh.anh", "minh.anh@gmail.com", Role.LEARNER);
        unverified.setEmailVerifiedAt(null);
        save(unverified);
        User banned = user("hung.ban", "hung.ban@gmail.com", Role.LEARNER);
        banned.setBannedAt(NOW.minus(Duration.ofDays(1)));
        save(banned);
        User banExpired = user("het.han", "het.han@gmail.com", Role.MODERATOR);
        banExpired.setBannedAt(NOW.minus(Duration.ofDays(10)));
        banExpired.setBannedUntil(NOW.minus(Duration.ofDays(3)));
        save(banExpired);
        User deleted = user("da.xoa", "da.xoa@gmail.com", Role.SETTER);
        deleted.setDeletedAt(NOW.minus(Duration.ofDays(2)));
        save(deleted);
        save(user("phan_tram", "phan.tram@gmail.com", Role.LEARNER));
    }

    @Test
    @DisplayName("Mặc định: chỉ tài khoản chưa xoá, mới tạo đứng trước, phân trang đúng tổng")
    void search_defaults_newestActiveAccountsPaged() {
        PageResponse<AdminUserSummaryResponse> firstPage = search(none(), 1, 2);
        PageResponse<AdminUserSummaryResponse> secondPage = search(none(), 2, 2);
        PageResponse<AdminUserSummaryResponse> lastPage = search(none(), 3, 2);

        assertThat(usernames(firstPage)).containsExactly("phan_tram", "het.han");
        assertThat(usernames(secondPage)).containsExactly("hung.ban", "minh.anh");
        assertThat(usernames(lastPage)).containsExactly("lan.pham");
        assertThat(firstPage.totalItems()).isEqualTo(5);
        assertThat(firstPage.totalPages()).isEqualTo(3);
        assertThat(firstPage.page()).isEqualTo(1);
        assertThat(firstPage.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("Tìm theo username hoặc email, bỏ khoảng trắng, không phân biệt hoa thường")
    void search_keyword_matchesUsernameOrEmail() {
        assertThat(usernames(search(keyword(" LAN "), 1, 20))).containsExactly("lan.pham");
        assertThat(usernames(search(keyword("anh@gmail"), 1, 20))).containsExactly("minh.anh");
    }

    @Test
    @DisplayName("Ký tự _ và % trong từ khoá được hiểu là chữ, không phải ký tự đại diện")
    void search_likeWildcards_areMatchedLiterally() {
        assertThat(usernames(search(keyword("_"), 1, 20))).containsExactly("phan_tram");
        assertThat(usernames(search(keyword("%"), 1, 20))).isEmpty();
    }

    @Test
    @DisplayName("Lọc theo vai trò, trạng thái khoá (tính cả hạn khoá), xác minh email, đã xoá")
    void search_filters_combineCorrectly() {
        assertThat(usernames(search(new AdminUserSearch(null, Role.SETTER, null, null, false))))
                .containsExactly("lan.pham");
        assertThat(
                        usernames(
                                search(
                                        new AdminUserSearch(
                                                null, null, AccountStatus.BANNED, null, false))))
                .containsExactly("hung.ban");
        assertThat(
                        usernames(
                                search(
                                        new AdminUserSearch(
                                                null, null, AccountStatus.ACTIVE, true, false))))
                .containsExactly("phan_tram", "het.han", "lan.pham");
        assertThat(
                        usernames(
                                search(
                                        new AdminUserSearch(
                                                null, null, AccountStatus.ACTIVE, false, false))))
                .containsExactly("minh.anh");
        assertThat(usernames(search(new AdminUserSearch(null, null, null, null, true))))
                .containsExactly("da.xoa");
    }

    @Test
    @DisplayName(
            "Mỗi dòng có trạng thái, hạn khoá, ngày tham gia; số liệu của service khác tạm là 0")
    void search_row_mapsAccountFields() {
        AdminUserSummaryResponse banned = search(keyword("hung.ban"), 1, 20).items().get(0);
        AdminUserSummaryResponse deleted =
                search(new AdminUserSearch("da.xoa", null, null, null, true)).items().get(0);

        assertThat(banned.status()).isEqualTo(AccountStatus.BANNED);
        assertThat(banned.bannedUntil()).isNull();
        assertThat(banned.isEmailVerified()).isTrue();
        assertThat(banned.joinedAt()).isNotNull();
        assertThat(banned.lastActiveAt()).isNull();
        assertThat(banned.rating()).isZero();
        assertThat(banned.acceptedCount()).isZero();
        assertThat(banned.isOnline()).isFalse();
        assertThat(banned.avatarUrl()).isNull();
        assertThat(deleted.deletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Tóm tắt chỉ đếm tài khoản chưa xoá; chưa xác minh không tính người đang bị khoá")
    void countUsers_countsLiveAccounts() {
        UserCountsResponse counts = service.countUsers();

        assertThat(counts).isEqualTo(new UserCountsResponse(5, 1, 1, 1, 1));
    }

    @Test
    @DisplayName(
            "Chi tiết theo username gồm tên hiển thị, cách đăng ký; số liệu của service khác tạm"
                    + " rỗng")
    void getUser_existingUsername_returnsDetail() {
        User google = user("thu.google", "thu@gmail.com", Role.LEARNER);
        google.setDisplayName("Nguyễn Thu");
        google.setSignupProvider(SignupProvider.GOOGLE);
        save(google);

        AdminUserDetailResponse detail = service.getUser("thu.google");

        assertThat(detail.account().username()).isEqualTo("thu.google");
        assertThat(detail.account().status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(detail.displayName()).isEqualTo("Nguyễn Thu");
        assertThat(detail.signupProvider()).isEqualTo(SignupProvider.GOOGLE);
        assertThat(detail.school()).isNull();
        assertThat(detail.submissionCount()).isZero();
        assertThat(detail.contestCount()).isZero();
        assertThat(detail.violations()).isEmpty();
    }

    @Test
    @DisplayName("Tài khoản đã xoá và đang bị khoá vẫn xem được chi tiết")
    void getUser_deletedOrBannedAccount_isStillVisible() {
        assertThat(service.getUser("da.xoa").account().deletedAt()).isNotNull();
        assertThat(service.getUser("hung.ban").account().status()).isEqualTo(AccountStatus.BANNED);
    }

    @Test
    @DisplayName("Username không tồn tại ném NotFoundException USER_NOT_FOUND")
    void getUser_unknownUsername_throwsNotFound() {
        assertThatThrownBy(() -> service.getUser("khong.co"))
                .isInstanceOfSatisfying(
                        NotFoundException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
    }

    private PageResponse<AdminUserSummaryResponse> search(AdminUserSearch search) {
        return search(search, 1, 20);
    }

    private PageResponse<AdminUserSummaryResponse> search(
            AdminUserSearch search, int page, int size) {
        return service.search(search, page, size);
    }

    private static AdminUserSearch none() {
        return new AdminUserSearch(null, null, null, null, false);
    }

    private static AdminUserSearch keyword(String q) {
        return new AdminUserSearch(q, null, null, null, false);
    }

    private static List<String> usernames(PageResponse<AdminUserSummaryResponse> page) {
        return page.items().stream().map(AdminUserSummaryResponse::username).toList();
    }

    private void save(User user) {
        userRepository.saveAndFlush(user);
    }

    private static User user(String username, String email, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setRole(role);
        user.setSignupProvider(SignupProvider.EMAIL);
        user.setEmailVerifiedAt(NOW.minus(Duration.ofDays(30)));
        return user;
    }
}
