package com.sqljudge.auth.unit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.jwk.RSAKey;
import com.sqljudge.auth.config.JwtKeyConfig;
import com.sqljudge.auth.config.SecurityConfig;
import com.sqljudge.auth.controller.AdminUserAvatarController;
import com.sqljudge.auth.dto.response.UserAvatarResponse;
import com.sqljudge.auth.service.AdminUserAvatarService;
import com.sqljudge.common.config.CommonAutoConfiguration;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.InvalidFieldException;
import com.sqljudge.common.exception.ServiceUnavailableException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.multipart.MultipartFile;

@WebMvcTest(AdminUserAvatarController.class)
@Import({SecurityConfig.class, JwtKeyConfig.class})
@ImportAutoConfiguration(CommonAutoConfiguration.class)
@TestPropertySource(
        properties = {
            "sqljudge.jwt.generate-ephemeral-key=true",
            "sqljudge.security.allowed-origins=http://localhost:3000"
        })
class AdminUserAvatarControllerTest {

    private static final String AVATAR_PATH = "/api/admin/users/viet.luu/avatar";
    private static final MockMultipartFile PNG_FILE =
            new MockMultipartFile("file", "anh.png", "image/png", AvatarUploadsTest.PNG);

    @Autowired private MockMvc mockMvc;
    @Autowired private RSAKey signingKey;

    @MockitoBean private AdminUserAvatarService adminUserAvatarService;

    @Test
    @DisplayName("Tải avatar trả 200 kèm avatarUrl, người thực hiện lấy từ JWT")
    void replace_admin_returnsAvatarUrl() throws Exception {
        when(adminUserAvatarService.replace(
                        eq("viet.luu"), any(MultipartFile.class), eq(TestJwts.USER_ID)))
                .thenReturn(new UserAvatarResponse("https://res.cloudinary.com/x/moi.png"));

        mockMvc.perform(as("ADMIN", upload(PNG_FILE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value("https://res.cloudinary.com/x/moi.png"));
    }

    @Test
    @DisplayName("File sai trả 400 trên field file; kho ảnh lỗi trả 503")
    void replace_invalidOrUnavailable() throws Exception {
        when(adminUserAvatarService.replace(eq("viet.luu"), any(), eq(TestJwts.USER_ID)))
                .thenThrow(new InvalidFieldException("file", "Ảnh phải là PNG, JPG hoặc WEBP"))
                .thenThrow(
                        new ServiceUnavailableException(
                                ErrorCode.AVATAR_STORAGE_UNAVAILABLE, "Chưa lưu được ảnh"));

        mockMvc.perform(as("ADMIN", upload(PNG_FILE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("file"));
        mockMvc.perform(as("ADMIN", upload(PNG_FILE)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AVATAR_STORAGE_UNAVAILABLE"));
    }

    @Test
    @DisplayName("Không phải ADMIN bị 403, chưa đăng nhập bị 401")
    void replace_requiresAdminRole() throws Exception {
        for (String role : List.of("LEARNER", "SETTER", "MODERATOR")) {
            mockMvc.perform(as(role, upload(PNG_FILE))).andExpect(status().isForbidden());
        }
        mockMvc.perform(upload(PNG_FILE)).andExpect(status().isUnauthorized());

        verifyNoInteractions(adminUserAvatarService);
    }

    private static MockHttpServletRequestBuilder upload(MockMultipartFile file) {
        return multipart(HttpMethod.PUT, AVATAR_PATH).file(file);
    }

    private MockHttpServletRequestBuilder as(String role, MockHttpServletRequestBuilder request) {
        return request.header(
                HttpHeaders.AUTHORIZATION, "Bearer " + TestJwts.validToken(signingKey, role));
    }
}
