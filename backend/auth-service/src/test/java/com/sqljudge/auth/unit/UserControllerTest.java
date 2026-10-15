package com.sqljudge.auth.unit;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sqljudge.auth.config.JwtKeyConfig;
import com.sqljudge.auth.config.SecurityConfig;
import com.sqljudge.auth.controller.UserController;
import com.sqljudge.auth.dto.response.UsernameAvailabilityResponse;
import com.sqljudge.auth.service.UserService;
import com.sqljudge.common.config.CommonAutoConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtKeyConfig.class})
@ImportAutoConfiguration(CommonAutoConfiguration.class)
@TestPropertySource(properties = "sqljudge.jwt.generate-ephemeral-key=true")
class UserControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private UserService userService;

    @Test
    @DisplayName("Khách chưa đăng nhập kiểm tra được username còn trống")
    void availability_anonymous_returnsAvailability() throws Exception {
        when(userService.checkUsernameAvailability("minh.anh"))
                .thenReturn(new UsernameAvailabilityResponse(true));

        mockMvc.perform(get("/api/users/availability").param("username", "minh.anh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    @DisplayName("Username sai mẫu trả 400 kèm lỗi ở trường username, không hỏi service")
    void availability_invalidUsername_returnsValidationError() throws Exception {
        mockMvc.perform(get("/api/users/availability").param("username", "Minh Anh"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("username"))
                .andExpect(
                        jsonPath("$.errors[0].message")
                                .value(
                                        "Tên người dùng gồm 3-30 ký tự: chữ thường, số, dấu chấm"
                                                + " hoặc gạch dưới"));

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("Thiếu tham số username trả 400")
    void availability_missingUsername_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/users/availability"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    @DisplayName("Chỉ mở GET: phương thức khác trên cùng đường dẫn vẫn cần đăng nhập")
    void availability_otherMethod_requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/users/availability").param("username", "minh.anh").with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}
