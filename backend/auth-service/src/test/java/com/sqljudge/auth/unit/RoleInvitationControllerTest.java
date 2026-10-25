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
import com.sqljudge.auth.controller.RoleInvitationController;
import com.sqljudge.auth.dto.response.AcceptInvitationResponse;
import com.sqljudge.auth.dto.response.RoleInvitationViewResponse;
import com.sqljudge.auth.entity.InvitationStatus;
import com.sqljudge.auth.service.RoleInvitationAcceptanceService;
import com.sqljudge.common.config.CommonAutoConfiguration;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.ForbiddenException;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.common.security.CurrentUser;
import com.sqljudge.common.security.Role;
import java.time.Instant;
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

@WebMvcTest(RoleInvitationController.class)
@Import({SecurityConfig.class, JwtKeyConfig.class})
@ImportAutoConfiguration(CommonAutoConfiguration.class)
@TestPropertySource(
        properties = {
            "sqljudge.jwt.generate-ephemeral-key=true",
            "sqljudge.security.allowed-origins=http://localhost:3000"
        })
class RoleInvitationControllerTest {

    private static final String TOKEN = "AbCdEfGhIjKlMnOpQrStUvWxYz0123456789_-abcde";
    private static final String PATH = "/api/role-invitations/" + TOKEN;

    @Autowired private MockMvc mockMvc;
    @Autowired private RSAKey signingKey;

    @MockitoBean private RoleInvitationAcceptanceService acceptanceService;

    @Test
    @DisplayName(
            "Xem lời mời không cần đăng nhập: trả email đã che, vai trò, người mời, trạng thái")
    void view_anonymous_returnsInvitation() throws Exception {
        when(acceptanceService.view(TOKEN))
                .thenReturn(
                        new RoleInvitationViewResponse(
                                "vi***@gmail.com",
                                Role.SETTER,
                                "admin.huy",
                                Instant.parse("2026-11-01T05:00:00Z"),
                                InvitationStatus.PENDING));

        mockMvc.perform(get(PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maskedEmail").value("vi***@gmail.com"))
                .andExpect(jsonPath("$.role").value("SETTER"))
                .andExpect(jsonPath("$.inviterUsername").value("admin.huy"))
                .andExpect(jsonPath("$.expiresAt").value("2026-11-01T05:00:00Z"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("Token không tồn tại: 404 INVITATION_NOT_FOUND")
    void view_unknownToken_returnsNotFound() throws Exception {
        when(acceptanceService.view(TOKEN))
                .thenThrow(
                        new NotFoundException(
                                ErrorCode.INVITATION_NOT_FOUND, "Không tìm thấy lời mời"));

        mockMvc.perform(get(PATH))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INVITATION_NOT_FOUND"));
    }

    @Test
    @DisplayName("Nhận lời mời khi đã đăng nhập: 200 kèm vai trò mới, dùng đúng người đăng nhập")
    void accept_signedIn_returnsNewRole() throws Exception {
        when(acceptanceService.accept(eq(TOKEN), any(CurrentUser.class)))
                .thenReturn(new AcceptInvitationResponse(Role.SETTER));

        mockMvc.perform(as("LEARNER", post(PATH + "/accept")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("SETTER"));

        ArgumentCaptor<CurrentUser> actor = ArgumentCaptor.forClass(CurrentUser.class);
        verify(acceptanceService).accept(eq(TOKEN), actor.capture());
        assertThat(actor.getValue().id()).isEqualTo(TestJwts.USER_ID);
    }

    @Test
    @DisplayName("Email không khớp: 403 INVITATION_EMAIL_MISMATCH")
    void accept_emailMismatch_returnsForbidden() throws Exception {
        when(acceptanceService.accept(eq(TOKEN), any(CurrentUser.class)))
                .thenThrow(
                        new ForbiddenException(
                                ErrorCode.INVITATION_EMAIL_MISMATCH,
                                "Lời mời này gửi tới email khác"));

        mockMvc.perform(as("LEARNER", post(PATH + "/accept")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("INVITATION_EMAIL_MISMATCH"));
    }

    @Test
    @DisplayName("Nhận lời mời khi chưa đăng nhập: 401")
    void accept_anonymous_returnsUnauthorized() throws Exception {
        mockMvc.perform(post(PATH + "/accept")).andExpect(status().isUnauthorized());

        verifyNoInteractions(acceptanceService);
    }

    private MockHttpServletRequestBuilder as(String role, MockHttpServletRequestBuilder request) {
        return request.header(
                HttpHeaders.AUTHORIZATION, "Bearer " + TestJwts.validToken(signingKey, role));
    }
}
