package com.sqljudge.auth.unit;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sqljudge.auth.config.SecurityConfig;
import com.sqljudge.common.config.CommonAutoConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@Import(SecurityConfig.class)
@ImportAutoConfiguration(CommonAutoConfiguration.class)
class SecurityConfigTest {

    @Autowired private MockMvc mockMvc;

    @Test
    @DisplayName("Gọi API khi chưa đăng nhập trả 401 dạng ApiError")
    void protectedPath_anonymous_returnsUnauthorizedApiError() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Bạn cần đăng nhập để tiếp tục"));
    }

    @Test
    @DisplayName("Đã đăng nhập thì đi qua lớp bảo mật tới tầng MVC")
    @WithMockUser
    void protectedPath_authenticated_passesSecurity() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("Health check và tài liệu API không cần đăng nhập")
    void publicPaths_anonymous_areNotBlockedBySecurity() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().is(not(401)));
        mockMvc.perform(get("/actuator/prometheus")).andExpect(status().is(not(401)));
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().is(not(401)));
    }
}
