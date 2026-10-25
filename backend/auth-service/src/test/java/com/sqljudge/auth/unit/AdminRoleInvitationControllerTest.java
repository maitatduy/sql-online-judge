package com.sqljudge.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.jwk.RSAKey;
import com.sqljudge.auth.config.JwtKeyConfig;
import com.sqljudge.auth.config.SecurityConfig;
import com.sqljudge.auth.controller.AdminRoleInvitationController;
import com.sqljudge.auth.dto.request.InviteRolesRequest;
import com.sqljudge.auth.dto.response.InviteRolesResponse;
import com.sqljudge.auth.dto.response.RoleInvitationSummaryResponse;
import com.sqljudge.auth.dto.response.SkippedInvitation;
import com.sqljudge.auth.entity.InvitationStatus;
import com.sqljudge.auth.service.RoleInvitationAdminService;
import com.sqljudge.auth.service.RoleInvitationSearch;
import com.sqljudge.auth.service.RoleInvitationService;
import com.sqljudge.common.config.CommonAutoConfiguration;
import com.sqljudge.common.security.CurrentUser;
import com.sqljudge.common.security.Role;
import com.sqljudge.common.web.PageResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
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

@WebMvcTest(AdminRoleInvitationController.class)
@Import({SecurityConfig.class, JwtKeyConfig.class})
@ImportAutoConfiguration(CommonAutoConfiguration.class)
@TestPropertySource(
        properties = {
            "sqljudge.jwt.generate-ephemeral-key=true",
            "sqljudge.security.allowed-origins=http://localhost:3000"
        })
class AdminRoleInvitationControllerTest {

    private static final String PATH = "/api/admin/role-invitations";
    private static final UUID INVITATION_ID =
            UUID.fromString("0192f3a0-0000-7000-8000-0000000000f1");
    private static final RoleInvitationSummaryResponse SUMMARY =
            new RoleInvitationSummaryResponse(
                    INVITATION_ID,
                    "an@gmail.com",
                    Role.SETTER,
                    InvitationStatus.PENDING,
                    "admin.huy",
                    Instant.parse("2026-10-24T05:00:00Z"),
                    Instant.parse("2026-10-31T05:00:00Z"),
                    null,
                    null);
    private static final String VALID_BODY =
            """
            {"emails":["viet.luu@gmail.com","moi@gmail.com"],"role":"SETTER",\
            "message":"Ra đề Weekly"}\
            """;

    @Autowired private MockMvc mockMvc;
    @Autowired private RSAKey signingKey;

    @MockitoBean private RoleInvitationService roleInvitationService;
    @MockitoBean private RoleInvitationAdminService roleInvitationAdminService;

    @Test
    @DisplayName("Danh sách lời mời: lọc trạng thái, từ khoá, trang; trả đủ trường theo chuẩn")
    void list_admin_returnsPage() throws Exception {
        when(roleInvitationAdminService.search(
                        new RoleInvitationSearch(InvitationStatus.PENDING, "gmail"), 2, 10))
                .thenReturn(new PageResponse<>(List.of(SUMMARY), 2, 10, 11, 2));

        mockMvc.perform(
                        as("ADMIN", get(PATH))
                                .param("status", "PENDING")
                                .param("q", "gmail")
                                .param("page", "2")
                                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(11))
                .andExpect(jsonPath("$.items[0].id").value(INVITATION_ID.toString()))
                .andExpect(jsonPath("$.items[0].email").value("an@gmail.com"))
                .andExpect(jsonPath("$.items[0].role").value("SETTER"))
                .andExpect(jsonPath("$.items[0].status").value("PENDING"))
                .andExpect(jsonPath("$.items[0].inviterUsername").value("admin.huy"))
                .andExpect(jsonPath("$.items[0].invitedAt").value("2026-10-24T05:00:00Z"))
                .andExpect(jsonPath("$.items[0].expiresAt").value("2026-10-31T05:00:00Z"));
    }

    @Test
    @DisplayName("Trạng thái lạ, trang hoặc cỡ trang sai trả 400")
    void list_invalidParameters_returnsBadRequest() throws Exception {
        mockMvc.perform(as("ADMIN", get(PATH)).param("status", "DONE"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(as("ADMIN", get(PATH)).param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("page"));
        mockMvc.perform(as("ADMIN", get(PATH)).param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("size"));

        verifyNoInteractions(roleInvitationAdminService);
    }

    @Test
    @DisplayName("Huỷ lời mời trả 204, gửi lại trả 200 kèm lời mời sau khi gia hạn")
    void revokeAndResend_admin_delegateWithActor() throws Exception {
        when(roleInvitationAdminService.resend(eq(INVITATION_ID), any(CurrentUser.class)))
                .thenReturn(SUMMARY);

        mockMvc.perform(as("ADMIN", post(PATH + "/" + INVITATION_ID + "/revoke")))
                .andExpect(status().isNoContent());
        mockMvc.perform(as("ADMIN", post(PATH + "/" + INVITATION_ID + "/resend")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        ArgumentCaptor<CurrentUser> actor = ArgumentCaptor.forClass(CurrentUser.class);
        verify(roleInvitationAdminService).revoke(eq(INVITATION_ID), actor.capture());
        assertThat(actor.getValue().id()).isEqualTo(TestJwts.USER_ID);
    }

    @Test
    @DisplayName(
            "Danh sách, huỷ, gửi lại đều cần ADMIN: 403 với vai trò khác, 401 khi chưa đăng nhập")
    void manage_requiresAdminRole() throws Exception {
        for (String role : List.of("LEARNER", "SETTER", "MODERATOR")) {
            mockMvc.perform(as(role, get(PATH))).andExpect(status().isForbidden());
            mockMvc.perform(as(role, post(PATH + "/" + INVITATION_ID + "/revoke")))
                    .andExpect(status().isForbidden());
            mockMvc.perform(as(role, post(PATH + "/" + INVITATION_ID + "/resend")))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get(PATH)).andExpect(status().isUnauthorized());

        verifyNoInteractions(roleInvitationAdminService);
    }

    @Test
    @DisplayName("Admin gửi lời mời hàng loạt: 200 kèm số đã gửi và người bị bỏ qua")
    void invite_admin_returnsResult() throws Exception {
        when(roleInvitationService.invite(any(InviteRolesRequest.class), any(CurrentUser.class)))
                .thenReturn(
                        new InviteRolesResponse(
                                1,
                                List.of(
                                        new SkippedInvitation(
                                                "moi@gmail.com", "Đang có lời mời chờ nhận"))));

        mockMvc.perform(as("ADMIN", json(VALID_BODY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sentCount").value(1))
                .andExpect(jsonPath("$.skipped[0].email").value("moi@gmail.com"))
                .andExpect(jsonPath("$.skipped[0].reason").value("Đang có lời mời chờ nhận"));

        ArgumentCaptor<InviteRolesRequest> request =
                ArgumentCaptor.forClass(InviteRolesRequest.class);
        ArgumentCaptor<CurrentUser> actor = ArgumentCaptor.forClass(CurrentUser.class);
        verify(roleInvitationService).invite(request.capture(), actor.capture());
        assertThat(request.getValue())
                .isEqualTo(
                        new InviteRolesRequest(
                                List.of("viet.luu@gmail.com", "moi@gmail.com"),
                                Role.SETTER,
                                "Ra đề Weekly"));
        assertThat(actor.getValue().id()).isEqualTo(TestJwts.USER_ID);
    }

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {
                "{\"emails\":[],\"role\":\"SETTER\"}|emails",
                "{\"emails\":[\"khong-phai-email\"],\"role\":\"SETTER\"}|emails[0]",
                "{\"emails\":[\"an@gmail.com\"]}|role",
            })
    @DisplayName("Danh sách rỗng, email sai hoặc thiếu vai trò trả 400 đúng trường")
    void invite_invalidBody_returnsFieldError(String body, String field) throws Exception {
        mockMvc.perform(as("ADMIN", json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value(field));

        verifyNoInteractions(roleInvitationService);
    }

    @Test
    @DisplayName("Quá 20 email hoặc lời nhắn quá 500 ký tự trả 400")
    void invite_tooManyEmailsOrLongMessage_returnsBadRequest() throws Exception {
        String emails =
                String.join(
                        ",",
                        java.util.stream.IntStream.range(0, 21)
                                .mapToObj(index -> "\"u" + index + "@gmail.com\"")
                                .toList());
        mockMvc.perform(as("ADMIN", json("{\"emails\":[" + emails + "],\"role\":\"SETTER\"}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("emails"));
        mockMvc.perform(
                        as(
                                "ADMIN",
                                json(
                                        "{\"emails\":[\"an@gmail.com\"],\"role\":\"SETTER\","
                                                + "\"message\":\""
                                                + "x".repeat(501)
                                                + "\"}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("message"));

        verifyNoInteractions(roleInvitationService);
    }

    @Test
    @DisplayName("Không phải ADMIN (kể cả MODERATOR) bị 403; chưa đăng nhập bị 401")
    void invite_requiresAdminRole() throws Exception {
        for (String role : List.of("LEARNER", "SETTER", "MODERATOR")) {
            mockMvc.perform(as(role, json(VALID_BODY))).andExpect(status().isForbidden());
        }
        mockMvc.perform(json(VALID_BODY)).andExpect(status().isUnauthorized());

        verifyNoInteractions(roleInvitationService);
    }

    private static MockHttpServletRequestBuilder json(String body) {
        return post(PATH).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private MockHttpServletRequestBuilder as(String role, MockHttpServletRequestBuilder request) {
        return request.header(
                HttpHeaders.AUTHORIZATION, "Bearer " + TestJwts.validToken(signingKey, role));
    }
}
