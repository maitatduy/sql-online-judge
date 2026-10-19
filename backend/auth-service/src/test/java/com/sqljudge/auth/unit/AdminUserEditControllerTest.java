package com.sqljudge.auth.unit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.jwk.RSAKey;
import com.sqljudge.auth.config.JwtKeyConfig;
import com.sqljudge.auth.config.SecurityConfig;
import com.sqljudge.auth.controller.AdminUserEditController;
import com.sqljudge.auth.dto.request.UpdateUserRequest;
import com.sqljudge.auth.dto.response.AccountStatus;
import com.sqljudge.auth.dto.response.AdminUserDetailResponse;
import com.sqljudge.auth.dto.response.AdminUserSummaryResponse;
import com.sqljudge.auth.entity.SignupProvider;
import com.sqljudge.auth.service.AdminUserEditService;
import com.sqljudge.common.config.CommonAutoConfiguration;
import com.sqljudge.common.security.Role;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(AdminUserEditController.class)
@Import({SecurityConfig.class, JwtKeyConfig.class})
@ImportAutoConfiguration(CommonAutoConfiguration.class)
@TestPropertySource(
        properties = {
            "sqljudge.jwt.generate-ephemeral-key=true",
            "sqljudge.security.allowed-origins=http://localhost:3000"
        })
class AdminUserEditControllerTest {

    private static final String USER_PATH = "/api/admin/users/viet.luu";
    private static final String ROLE_CHANGE = "{\"role\":\"SETTER\",\"reason\":\"Ra đề\"}";

    @Autowired private MockMvc mockMvc;
    @Autowired private RSAKey signingKey;

    @MockitoBean private AdminUserEditService adminUserEditService;

    @Test
    @DisplayName("Admin đổi vai trò: chuyển yêu cầu và người thực hiện từ JWT cho service")
    void update_admin_returnsUpdatedDetail() throws Exception {
        when(adminUserEditService.update(
                        eq("viet.luu"), any(UpdateUserRequest.class), eq(TestJwts.USER_ID)))
                .thenReturn(detail(Role.SETTER));

        mockMvc.perform(as("ADMIN", json(patch(USER_PATH), ROLE_CHANGE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("viet.luu"))
                .andExpect(jsonPath("$.role").value("SETTER"));
    }

    @ParameterizedTest(name = "{1}: {0}")
    @CsvSource(
            delimiter = '|',
            value = {
                "{}|role",
                "{\"role\":\"BOSS\"}|role",
                "{\"role\":null}|role",
                "{\"displayName\":\"   \"}|displayName"
            })
    @DisplayName("Dữ liệu sửa sai trả 400 VALIDATION_FAILED đúng trường")
    void update_invalidField_returnsFieldError(String body, String field) throws Exception {
        mockMvc.perform(as("ADMIN", json(patch(USER_PATH), body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value(field));

        verifyNoInteractions(adminUserEditService);
    }

    @Test
    @DisplayName("Không phải ADMIN bị 403, chưa đăng nhập bị 401")
    void update_requiresAdminRole() throws Exception {
        for (String role : List.of("LEARNER", "SETTER", "MODERATOR")) {
            mockMvc.perform(as(role, json(patch(USER_PATH), ROLE_CHANGE)))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(json(patch(USER_PATH), ROLE_CHANGE)).andExpect(status().isUnauthorized());

        verifyNoInteractions(adminUserEditService);
    }

    private static AdminUserDetailResponse detail(Role role) {
        return new AdminUserDetailResponse(
                new AdminUserSummaryResponse(
                        UUID.fromString("0192f3a0-0000-7000-8000-00000000000b"),
                        "viet.luu",
                        "viet.luu@gmail.com",
                        role,
                        0,
                        0,
                        Instant.parse("2026-09-01T00:00:00Z"),
                        null,
                        false,
                        AccountStatus.ACTIVE,
                        null,
                        true,
                        null,
                        null),
                null,
                null,
                SignupProvider.EMAIL,
                0,
                0,
                List.of());
    }

    private static MockHttpServletRequestBuilder json(
            MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private MockHttpServletRequestBuilder as(String role, MockHttpServletRequestBuilder request) {
        return request.header(
                HttpHeaders.AUTHORIZATION, "Bearer " + TestJwts.validToken(signingKey, role));
    }
}
