package com.sqljudge.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.jwk.RSAKey;
import com.sqljudge.auth.config.JwtKeyConfig;
import com.sqljudge.auth.config.SecurityConfig;
import com.sqljudge.auth.controller.AdminUserController;
import com.sqljudge.auth.dto.response.AccountStatus;
import com.sqljudge.auth.dto.response.AdminUserDetailResponse;
import com.sqljudge.auth.dto.response.AdminUserSummaryResponse;
import com.sqljudge.auth.dto.response.UserCountsResponse;
import com.sqljudge.auth.entity.SignupProvider;
import com.sqljudge.auth.service.AdminUserQueryService;
import com.sqljudge.auth.service.AdminUserSearch;
import com.sqljudge.common.config.CommonAutoConfiguration;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.common.security.Role;
import com.sqljudge.common.web.PageResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(AdminUserController.class)
@Import({SecurityConfig.class, JwtKeyConfig.class})
@ImportAutoConfiguration(CommonAutoConfiguration.class)
@TestPropertySource(
        properties = {
            "sqljudge.jwt.generate-ephemeral-key=true",
            "sqljudge.security.allowed-origins=http://localhost:3000"
        })
class AdminUserControllerTest {

    private static final AdminUserSummaryResponse SETTER =
            new AdminUserSummaryResponse(
                    UUID.fromString("0192f3a0-0000-7000-8000-000000000002"),
                    "lan.pham",
                    "lan.pham@gmail.com",
                    Role.SETTER,
                    0,
                    0,
                    Instant.parse("2026-09-01T00:00:00Z"),
                    null,
                    false,
                    AccountStatus.ACTIVE,
                    null,
                    true,
                    null,
                    null);

    @Autowired private MockMvc mockMvc;
    @Autowired private RSAKey signingKey;

    @MockitoBean private AdminUserQueryService adminUserQueryService;

    @Test
    @DisplayName(
            "Admin lọc danh sách: chuyển đúng điều kiện cho service, trả trang theo chuẩn phân"
                    + " trang")
    void listUsers_admin_returnsPage() throws Exception {
        when(adminUserQueryService.search(any(AdminUserSearch.class), eq(2), eq(20)))
                .thenReturn(new PageResponse<>(List.of(SETTER), 2, 20, 21, 2));

        mockMvc.perform(
                        as("ADMIN", get("/api/admin/users"))
                                .param("q", " Lan ")
                                .param("role", "SETTER")
                                .param("status", "ACTIVE")
                                .param("emailVerified", "true")
                                .param("page", "2")
                                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalItems").value(21))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.items[0].username").value("lan.pham"))
                .andExpect(jsonPath("$.items[0].joinedAt").value("2026-09-01T00:00:00Z"))
                .andExpect(jsonPath("$.items[0].lastActiveAt").isEmpty())
                .andExpect(jsonPath("$.items[0].isOnline").value(false))
                .andExpect(jsonPath("$.items[0].isEmailVerified").value(true))
                .andExpect(jsonPath("$.items[0].status").value("ACTIVE"));

        ArgumentCaptor<AdminUserSearch> search = ArgumentCaptor.forClass(AdminUserSearch.class);
        verify(adminUserQueryService).search(search.capture(), eq(2), eq(20));
        assertThat(search.getValue())
                .isEqualTo(
                        new AdminUserSearch(
                                " Lan ", Role.SETTER, AccountStatus.ACTIVE, true, false));
    }

    @Test
    @DisplayName("Không truyền gì: trang 1, 20 dòng, chỉ tài khoản chưa xoá")
    void listUsers_defaults_firstPageOfActiveAccounts() throws Exception {
        when(adminUserQueryService.search(
                        new AdminUserSearch(null, null, null, null, false), 1, 20))
                .thenReturn(new PageResponse<>(List.of(), 1, 20, 0, 0));

        mockMvc.perform(as("ADMIN", get("/api/admin/users")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    @DisplayName("Trang nhỏ hơn 1 hoặc size ngoài 1-100 trả 400 VALIDATION_FAILED theo trường")
    void listUsers_invalidPaging_returnsFieldErrors() throws Exception {
        mockMvc.perform(as("ADMIN", get("/api/admin/users")).param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("page"));
        mockMvc.perform(as("ADMIN", get("/api/admin/users")).param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("size"));

        verifyNoInteractions(adminUserQueryService);
    }

    @Test
    @DisplayName("Vai trò không tồn tại trả 400")
    void listUsers_unknownRole_returnsBadRequest() throws Exception {
        mockMvc.perform(as("ADMIN", get("/api/admin/users")).param("role", "OWNER"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(adminUserQueryService);
    }

    @Test
    @DisplayName("Admin xem số liệu tóm tắt")
    void userSummary_admin_returnsCounts() throws Exception {
        when(adminUserQueryService.countUsers()).thenReturn(new UserCountsResponse(12, 3, 2, 1, 4));

        mockMvc.perform(as("ADMIN", get("/api/admin/user-summary")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(12))
                .andExpect(jsonPath("$.unverified").value(3))
                .andExpect(jsonPath("$.setters").value(2))
                .andExpect(jsonPath("$.moderators").value(1))
                .andExpect(jsonPath("$.banned").value(4));
    }

    @Test
    @DisplayName(
            "Admin xem chi tiết: trường của dòng danh sách và trường chi tiết nằm cùng một cấp")
    void getUser_admin_returnsFlatDetail() throws Exception {
        when(adminUserQueryService.getUser("lan.pham"))
                .thenReturn(
                        new AdminUserDetailResponse(
                                SETTER, "Phạm Lan", null, SignupProvider.GOOGLE, 0, 0, List.of()));

        mockMvc.perform(as("ADMIN", get("/api/admin/users/lan.pham")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("lan.pham"))
                .andExpect(jsonPath("$.role").value("SETTER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.joinedAt").value("2026-09-01T00:00:00Z"))
                .andExpect(jsonPath("$.displayName").value("Phạm Lan"))
                .andExpect(jsonPath("$.school").isEmpty())
                .andExpect(jsonPath("$.signupProvider").value("GOOGLE"))
                .andExpect(jsonPath("$.submissionCount").value(0))
                .andExpect(jsonPath("$.contestCount").value(0))
                .andExpect(jsonPath("$.violations").isArray())
                .andExpect(jsonPath("$.violations").isEmpty())
                .andExpect(jsonPath("$.account").doesNotExist());
    }

    @Test
    @DisplayName("Username không tồn tại trả 404 USER_NOT_FOUND")
    void getUser_unknownUsername_returnsNotFound() throws Exception {
        when(adminUserQueryService.getUser("khong.co"))
                .thenThrow(
                        new NotFoundException(
                                ErrorCode.USER_NOT_FOUND, "Không tìm thấy người dùng"));

        mockMvc.perform(as("ADMIN", get("/api/admin/users/khong.co")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Không tìm thấy người dùng"));
    }

    @Test
    @DisplayName("Không phải ADMIN (kể cả MODERATOR) bị 403; chưa đăng nhập bị 401")
    void adminEndpoints_requireAdminRole() throws Exception {
        for (String role : List.of("LEARNER", "SETTER", "MODERATOR")) {
            mockMvc.perform(as(role, get("/api/admin/users"))).andExpect(status().isForbidden());
            mockMvc.perform(as(role, get("/api/admin/user-summary")))
                    .andExpect(status().isForbidden());
            mockMvc.perform(as(role, get("/api/admin/users/lan.pham")))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/user-summary")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/users/lan.pham")).andExpect(status().isUnauthorized());

        verify(adminUserQueryService, never()).countUsers();
        verify(adminUserQueryService, never()).search(any(), anyInt(), anyInt());
        verify(adminUserQueryService, never()).getUser(any());
    }

    private MockHttpServletRequestBuilder as(String role, MockHttpServletRequestBuilder request) {
        return request.header(
                HttpHeaders.AUTHORIZATION, "Bearer " + TestJwts.validToken(signingKey, role));
    }
}
