package com.sqljudge.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.jwk.RSAKey;
import com.sqljudge.auth.config.JwtKeyConfig;
import com.sqljudge.auth.config.SecurityConfig;
import com.sqljudge.auth.controller.AdminAuditLogController;
import com.sqljudge.auth.dto.response.AuditLogEntryResponse;
import com.sqljudge.auth.entity.AdminActionType;
import com.sqljudge.auth.service.AdminAuditLogService;
import com.sqljudge.auth.service.AuditLogSearch;
import com.sqljudge.common.config.CommonAutoConfiguration;
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

@WebMvcTest(AdminAuditLogController.class)
@Import({SecurityConfig.class, JwtKeyConfig.class})
@ImportAutoConfiguration(CommonAutoConfiguration.class)
@TestPropertySource(
        properties = {
            "sqljudge.jwt.generate-ephemeral-key=true",
            "sqljudge.security.allowed-origins=http://localhost:3000"
        })
class AdminAuditLogControllerTest {

    private static final String PATH = "/api/admin/audit-log";
    private static final AuditLogEntryResponse ROLE_CHANGE =
            new AuditLogEntryResponse(
                    UUID.fromString("0192f3a0-0000-7000-8000-0000000000e1"),
                    Instant.parse("2026-10-13T07:05:12Z"),
                    "admin.huy",
                    Role.ADMIN,
                    "datahust",
                    AdminActionType.UPDATE_ROLE,
                    "CLB được duyệt làm người ra đề",
                    "LEARNER",
                    "SETTER",
                    null,
                    null);

    @Autowired private MockMvc mockMvc;
    @Autowired private RSAKey signingKey;

    @MockitoBean private AdminAuditLogService adminAuditLogService;

    @Test
    @DisplayName("Admin lọc nhật ký: chuyển đúng điều kiện cho service, trả trang theo chuẩn")
    void listAuditLog_admin_returnsPage() throws Exception {
        ArgumentCaptor<AuditLogSearch> search = ArgumentCaptor.forClass(AuditLogSearch.class);
        when(adminAuditLogService.search(search.capture(), eq(2), eq(20)))
                .thenReturn(new PageResponse<>(List.of(ROLE_CHANGE), 2, 20, 21, 2));

        mockMvc.perform(
                        as("ADMIN", get(PATH))
                                .param("user", "datahust")
                                .param("actor", "admin.huy")
                                .param("action", "UPDATE_ROLE")
                                .param("from", "2026-10-06T17:00:00Z")
                                .param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(21))
                .andExpect(jsonPath("$.items[0].occurredAt").value("2026-10-13T07:05:12Z"))
                .andExpect(jsonPath("$.items[0].actorUsername").value("admin.huy"))
                .andExpect(jsonPath("$.items[0].actorRole").value("ADMIN"))
                .andExpect(jsonPath("$.items[0].targetUsername").value("datahust"))
                .andExpect(jsonPath("$.items[0].action").value("UPDATE_ROLE"))
                .andExpect(jsonPath("$.items[0].previousValue").value("LEARNER"))
                .andExpect(jsonPath("$.items[0].newValue").value("SETTER"));

        assertThat(search.getValue())
                .isEqualTo(
                        new AuditLogSearch(
                                "datahust",
                                "admin.huy",
                                AdminActionType.UPDATE_ROLE,
                                Instant.parse("2026-10-06T17:00:00Z")));
    }

    @Test
    @DisplayName("Không lọc gì: điều kiện rỗng, trang 1, 20 dòng")
    void listAuditLog_noFilter_usesDefaults() throws Exception {
        when(adminAuditLogService.search(new AuditLogSearch(null, null, null, null), 1, 20))
                .thenReturn(new PageResponse<>(List.of(), 1, 20, 0, 0));

        mockMvc.perform(as("ADMIN", get(PATH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());

        verify(adminAuditLogService).search(new AuditLogSearch(null, null, null, null), 1, 20);
    }

    @Test
    @DisplayName("Phân trang sai, loại thao tác hay mốc thời gian không hợp lệ trả 400")
    void listAuditLog_invalidParameters_returnsBadRequest() throws Exception {
        mockMvc.perform(as("ADMIN", get(PATH)).param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("page"));
        mockMvc.perform(as("ADMIN", get(PATH)).param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("size"));
        mockMvc.perform(as("ADMIN", get(PATH)).param("action", "DROP_TABLE"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(as("ADMIN", get(PATH)).param("from", "hôm qua"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(adminAuditLogService);
    }

    @Test
    @DisplayName("Không phải ADMIN (kể cả MODERATOR) bị 403; chưa đăng nhập bị 401")
    void listAuditLog_requiresAdminRole() throws Exception {
        for (String role : List.of("LEARNER", "SETTER", "MODERATOR")) {
            mockMvc.perform(as(role, get(PATH))).andExpect(status().isForbidden());
        }
        mockMvc.perform(get(PATH)).andExpect(status().isUnauthorized());

        verifyNoInteractions(adminAuditLogService);
    }

    private MockHttpServletRequestBuilder as(String role, MockHttpServletRequestBuilder request) {
        return request.header(
                HttpHeaders.AUTHORIZATION, "Bearer " + TestJwts.validToken(signingKey, role));
    }
}
