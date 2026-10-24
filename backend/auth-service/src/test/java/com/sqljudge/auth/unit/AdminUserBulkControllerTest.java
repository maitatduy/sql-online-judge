package com.sqljudge.auth.unit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.jwk.RSAKey;
import com.sqljudge.auth.config.JwtKeyConfig;
import com.sqljudge.auth.config.SecurityConfig;
import com.sqljudge.auth.controller.AdminUserBulkController;
import com.sqljudge.auth.dto.request.BulkBanUsersRequest;
import com.sqljudge.auth.dto.request.BulkChangeRoleRequest;
import com.sqljudge.auth.dto.response.BulkUserActionResponse;
import com.sqljudge.auth.service.AdminUserBulkService;
import com.sqljudge.common.config.CommonAutoConfiguration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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

@WebMvcTest(AdminUserBulkController.class)
@Import({SecurityConfig.class, JwtKeyConfig.class})
@ImportAutoConfiguration(CommonAutoConfiguration.class)
@TestPropertySource(
        properties = {
            "sqljudge.jwt.generate-ephemeral-key=true",
            "sqljudge.security.allowed-origins=http://localhost:3000"
        })
class AdminUserBulkControllerTest {

    private static final String BULK_ROLE_PATH = "/api/admin/users/bulk-role";
    private static final String VALID_ROLE_BODY =
            """
            {"usernames": ["an.bui", "admin.khac"], "role": "SETTER", "reason": "Cộng tác"}
            """;
    private static final String TOO_LONG_USERNAME = "a".repeat(31);
    private static final String BULK_BAN_PATH = "/api/admin/users/bulk-ban";
    private static final String VALID_BAN_BODY =
            """
            {"usernames": ["an.bui", "binh.tran"], "duration": "DAYS_30", "reason": "Spam",
             "notifyByEmail": true}
            """;

    @Autowired private MockMvc mockMvc;
    @Autowired private RSAKey signingKey;

    @MockitoBean private AdminUserBulkService adminUserBulkService;

    @Test
    @DisplayName("Đổi vai trò hàng loạt trả 200 kèm số tài khoản đã đổi và danh sách bỏ qua")
    void bulkRole_admin_returnsResult() throws Exception {
        when(adminUserBulkService.changeRole(
                        any(BulkChangeRoleRequest.class), eq(TestJwts.USER_ID)))
                .thenReturn(new BulkUserActionResponse(1, List.of("admin.khac")));

        mockMvc.perform(as("ADMIN", json(BULK_ROLE_PATH, VALID_ROLE_BODY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.affectedCount").value(1))
                .andExpect(jsonPath("$.skippedUsernames[0]").value("admin.khac"));
    }

    @Test
    @DisplayName("Khoá hàng loạt trả 200 kèm số tài khoản đã khoá và danh sách bỏ qua")
    void bulkBan_admin_returnsResult() throws Exception {
        when(adminUserBulkService.ban(any(BulkBanUsersRequest.class), eq(TestJwts.USER_ID)))
                .thenReturn(new BulkUserActionResponse(2, List.of()));

        mockMvc.perform(as("ADMIN", json(BULK_BAN_PATH, VALID_BAN_BODY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.affectedCount").value(2))
                .andExpect(jsonPath("$.skippedUsernames").isEmpty());
    }

    static List<Arguments> invalidBanBodies() {
        String tooMany =
                IntStream.rangeClosed(1, 101)
                        .mapToObj(index -> "\"user" + index + "\"")
                        .collect(Collectors.joining(","));
        return List.of(
                Arguments.of(
                        "{\"usernames\": [], \"duration\": \"DAYS_7\", \"reason\": \"x\","
                                + " \"notifyByEmail\": true}",
                        "usernames"),
                Arguments.of(
                        "{\"usernames\": ["
                                + tooMany
                                + "], \"duration\": \"DAYS_7\", \"reason\": \"x\","
                                + " \"notifyByEmail\": true}",
                        "usernames"),
                Arguments.of(
                        "{\"usernames\": [\""
                                + TOO_LONG_USERNAME
                                + "\"], \"duration\": \"DAYS_7\", \"reason\": \"x\","
                                + " \"notifyByEmail\": true}",
                        "usernames[0]"),
                Arguments.of(
                        "{\"usernames\": [\"an.bui\"], \"reason\": \"x\", \"notifyByEmail\":"
                                + " true}",
                        "duration"),
                Arguments.of(
                        "{\"usernames\": [\"an.bui\"], \"duration\": \"DAYS_7\", \"reason\": \"\","
                                + " \"notifyByEmail\": true}",
                        "reason"),
                Arguments.of(
                        "{\"usernames\": [\"an.bui\"], \"duration\": \"DAYS_7\", \"reason\":"
                                + " \"x\"}",
                        "notifyByEmail"));
    }

    @ParameterizedTest
    @MethodSource("invalidBanBodies")
    @DisplayName("Dữ liệu khoá hàng loạt sai: 400 kèm lỗi đúng trường, không khoá ai")
    void bulkBan_invalidBody_returnsFieldError(String body, String field) throws Exception {
        mockMvc.perform(as("ADMIN", json(BULK_BAN_PATH, body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value(field));

        verifyNoInteractions(adminUserBulkService);
    }

    @Test
    @DisplayName("Khoá hàng loạt: không phải ADMIN bị 403, chưa đăng nhập bị 401")
    void bulkBan_requiresAdminRole() throws Exception {
        for (String role : List.of("LEARNER", "SETTER", "MODERATOR")) {
            mockMvc.perform(as(role, json(BULK_BAN_PATH, VALID_BAN_BODY)))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(json(BULK_BAN_PATH, VALID_BAN_BODY)).andExpect(status().isUnauthorized());

        verifyNoInteractions(adminUserBulkService);
    }

    static List<Arguments> invalidRoleBodies() {
        String tooMany =
                IntStream.rangeClosed(1, 101)
                        .mapToObj(index -> "\"user" + index + "\"")
                        .collect(Collectors.joining(","));
        return List.of(
                Arguments.of(
                        "{\"usernames\": [], \"role\": \"SETTER\", \"reason\": \"x\"}",
                        "usernames"),
                Arguments.of("{\"role\": \"SETTER\", \"reason\": \"x\"}", "usernames"),
                Arguments.of(
                        "{\"usernames\": ["
                                + tooMany
                                + "], \"role\": \"SETTER\", \"reason\": \"x\"}",
                        "usernames"),
                Arguments.of(
                        "{\"usernames\": [\""
                                + TOO_LONG_USERNAME
                                + "\"], \"role\": \"SETTER\", \"reason\": \"x\"}",
                        "usernames[0]"),
                Arguments.of("{\"usernames\": [\"an.bui\"], \"reason\": \"x\"}", "role"),
                Arguments.of(
                        "{\"usernames\": [\"an.bui\"], \"role\": \"ADMIN\", \"reason\": \"x\"}",
                        "role"),
                Arguments.of(
                        "{\"usernames\": [\"an.bui\"], \"role\": \"SETTER\", \"reason\": \" \"}",
                        "reason"),
                Arguments.of(
                        "{\"usernames\": [\"an.bui\"], \"role\": \"SETTER\", \"reason\": \""
                                + "a".repeat(501)
                                + "\"}",
                        "reason"));
    }

    @ParameterizedTest
    @MethodSource("invalidRoleBodies")
    @DisplayName("Dữ liệu đổi vai trò hàng loạt sai: 400 kèm lỗi đúng trường, không đổi gì")
    void bulkRole_invalidBody_returnsFieldError(String body, String field) throws Exception {
        mockMvc.perform(as("ADMIN", json(BULK_ROLE_PATH, body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value(field));

        verifyNoInteractions(adminUserBulkService);
    }

    @Test
    @DisplayName("Đổi vai trò hàng loạt: không phải ADMIN bị 403, chưa đăng nhập bị 401")
    void bulkRole_requiresAdminRole() throws Exception {
        for (String role : List.of("LEARNER", "SETTER", "MODERATOR")) {
            mockMvc.perform(as(role, json(BULK_ROLE_PATH, VALID_ROLE_BODY)))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(json(BULK_ROLE_PATH, VALID_ROLE_BODY)).andExpect(status().isUnauthorized());

        verifyNoInteractions(adminUserBulkService);
    }

    private static MockHttpServletRequestBuilder json(String path, String body) {
        return post(path).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private MockHttpServletRequestBuilder as(String role, MockHttpServletRequestBuilder request) {
        return request.header(
                HttpHeaders.AUTHORIZATION, "Bearer " + TestJwts.validToken(signingKey, role));
    }
}
