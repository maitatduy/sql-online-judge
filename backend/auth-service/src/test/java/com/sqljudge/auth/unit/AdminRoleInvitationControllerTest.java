package com.sqljudge.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.jwk.RSAKey;
import com.sqljudge.auth.config.JwtKeyConfig;
import com.sqljudge.auth.config.SecurityConfig;
import com.sqljudge.auth.controller.AdminRoleInvitationController;
import com.sqljudge.auth.dto.request.InviteRolesRequest;
import com.sqljudge.auth.dto.response.InviteRolesResponse;
import com.sqljudge.auth.dto.response.SkippedInvitation;
import com.sqljudge.auth.service.RoleInvitationService;
import com.sqljudge.common.config.CommonAutoConfiguration;
import com.sqljudge.common.security.CurrentUser;
import com.sqljudge.common.security.Role;
import java.util.List;
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
    private static final String VALID_BODY =
            """
            {"emails":["viet.luu@gmail.com","moi@gmail.com"],"role":"SETTER",\
            "message":"Ra đề Weekly"}\
            """;

    @Autowired private MockMvc mockMvc;
    @Autowired private RSAKey signingKey;

    @MockitoBean private RoleInvitationService roleInvitationService;

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
