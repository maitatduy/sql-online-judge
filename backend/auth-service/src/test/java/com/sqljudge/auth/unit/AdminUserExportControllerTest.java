package com.sqljudge.auth.unit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.jwk.RSAKey;
import com.sqljudge.auth.config.JwtKeyConfig;
import com.sqljudge.auth.config.SecurityConfig;
import com.sqljudge.auth.controller.AdminUserExportController;
import com.sqljudge.auth.dto.request.ExportScope;
import com.sqljudge.auth.dto.request.ExportUsersRequest;
import com.sqljudge.auth.service.AdminUserExportService;
import com.sqljudge.auth.service.UserExport;
import com.sqljudge.common.config.CommonAutoConfiguration;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(AdminUserExportController.class)
@Import({
    SecurityConfig.class,
    JwtKeyConfig.class,
    AdminUserExportControllerTest.FixedClockConfig.class
})
@ImportAutoConfiguration(CommonAutoConfiguration.class)
@TestPropertySource(
        properties = {
            "sqljudge.jwt.generate-ephemeral-key=true",
            "sqljudge.security.allowed-origins=http://localhost:3000"
        })
class AdminUserExportControllerTest {

    private static final Instant NOW = Instant.parse("2026-10-20T05:00:00Z");
    private static final String EXPORT_PATH = "/api/admin/user-exports";
    private static final String VALID_BODY = "{\"scope\":\"ALL\",\"includeEmail\":false}";
    private static final String CSV = "username,role,status,joinedAt\nviet.luu,SETTER,ACTIVE,x\n";

    @Autowired private MockMvc mockMvc;
    @Autowired private RSAKey signingKey;

    @MockitoBean private AdminUserExportService adminUserExportService;

    @Test
    @DisplayName("Xuất CSV trả 200 text/csv, tải về dạng file đính kèm có tên theo thời điểm xuất")
    void export_admin_streamsCsvAttachment() throws Exception {
        UserExport export =
                new UserExport(
                        new ExportUsersRequest(ExportScope.ALL, null, null, false, null),
                        NOW,
                        List.of());
        when(adminUserExportService.prepare(
                        any(ExportUsersRequest.class), eq(TestJwts.USER_ID), eq(NOW)))
                .thenReturn(export);
        doAnswer(
                        invocation -> {
                            OutputStream out = invocation.getArgument(1);
                            out.write(CSV.getBytes(StandardCharsets.UTF_8));
                            return null;
                        })
                .when(adminUserExportService)
                .write(eq(export), any(OutputStream.class));

        MvcResult started =
                mockMvc.perform(as("ADMIN", json(VALID_BODY)))
                        .andExpect(request().asyncStarted())
                        .andReturn();

        mockMvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv;charset=UTF-8"))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_DISPOSITION,
                                        "attachment; filename=\"users-20261020-0500.csv\""))
                .andExpect(content().string(CSV));
    }

    @Test
    @DisplayName("Yêu cầu xuất sai: 400 kèm lỗi đúng trường, không xuất gì")
    void export_invalidBody_returnsFieldError() throws Exception {
        mockMvc.perform(as("ADMIN", json("{\"scope\":\"ALL\",\"includeEmail\":true}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("reason"));
        mockMvc.perform(as("ADMIN", json("{\"scope\":\"FILTERED\",\"includeEmail\":false}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("filters"));

        verifyNoInteractions(adminUserExportService);
    }

    @Test
    @DisplayName("Xuất CSV: không phải ADMIN bị 403, chưa đăng nhập bị 401")
    void export_requiresAdminRole() throws Exception {
        for (String role : List.of("LEARNER", "SETTER", "MODERATOR")) {
            mockMvc.perform(as(role, json(VALID_BODY))).andExpect(status().isForbidden());
        }
        mockMvc.perform(json(VALID_BODY)).andExpect(status().isUnauthorized());

        verifyNoInteractions(adminUserExportService);
    }

    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    private static MockHttpServletRequestBuilder json(String body) {
        return post(EXPORT_PATH).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private MockHttpServletRequestBuilder as(String role, MockHttpServletRequestBuilder request) {
        return request.header(
                HttpHeaders.AUTHORIZATION, "Bearer " + TestJwts.validToken(signingKey, role));
    }
}
