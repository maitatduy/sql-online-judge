package com.sqljudge.common.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.sqljudge.common.web.ApiErrorWriter;
import com.sqljudge.common.web.RestAccessDeniedHandler;
import com.sqljudge.common.web.RestAuthenticationEntryPoint;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;

class RestSecurityHandlersTest {

    private final ApiErrorWriter writer =
            new ApiErrorWriter(JsonMapper.builder().findAndAddModules().build());
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-11T13:05:22Z"), ZoneOffset.UTC);

    @Test
    @DisplayName("Chưa đăng nhập: filter bảo mật trả 401 dạng ApiError JSON")
    void entryPoint_writesUnauthorizedApiError() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new RestAuthenticationEntryPoint(writer, clock)
                .commence(
                        new MockHttpServletRequest(),
                        response,
                        new InsufficientAuthenticationException("missing token"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString())
                .contains("\"code\":\"UNAUTHORIZED\"")
                .contains("Bạn cần đăng nhập để tiếp tục");
    }

    @Test
    @DisplayName("Thiếu quyền: filter bảo mật trả 403 dạng ApiError JSON")
    void accessDeniedHandler_writesForbiddenApiError() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new RestAccessDeniedHandler(writer, clock)
                .handle(
                        new MockHttpServletRequest(),
                        response,
                        new AccessDeniedException("denied"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("\"code\":\"ACCESS_DENIED\"");
    }
}
