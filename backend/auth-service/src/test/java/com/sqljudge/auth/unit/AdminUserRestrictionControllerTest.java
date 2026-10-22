package com.sqljudge.auth.unit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.jwk.RSAKey;
import com.sqljudge.auth.config.JwtKeyConfig;
import com.sqljudge.auth.config.SecurityConfig;
import com.sqljudge.auth.controller.AdminUserRestrictionController;
import com.sqljudge.auth.dto.request.UpdateRestrictionsRequest;
import com.sqljudge.auth.dto.response.UserRestrictionsResponse;
import com.sqljudge.auth.entity.UserRestriction;
import com.sqljudge.auth.service.AdminUserRestrictionService;
import com.sqljudge.common.config.CommonAutoConfiguration;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.BusinessRuleException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(AdminUserRestrictionController.class)
@Import({SecurityConfig.class, JwtKeyConfig.class})
@ImportAutoConfiguration(CommonAutoConfiguration.class)
@TestPropertySource(
        properties = {
            "sqljudge.jwt.generate-ephemeral-key=true",
            "sqljudge.security.allowed-origins=http://localhost:3000"
        })
class AdminUserRestrictionControllerTest {

    private static final String PATH = "/api/admin/users/viet.luu/restrictions";
    private static final String VALID_BODY = "{\"aiDailyQuota\":5,\"reason\":\"Lạm dụng AI\"}";

    @Autowired private MockMvc mockMvc;
    @Autowired private RSAKey signingKey;

    @MockitoBean private AdminUserRestrictionService adminUserRestrictionService;

    @Test
    @DisplayName("Xem hạn chế trả 200 với hạn mức AI và cờ cấm contest có rating")
    void get_admin_returnsRestrictions() throws Exception {
        when(adminUserRestrictionService.get("viet.luu"))
                .thenReturn(new UserRestrictionsResponse(20, false));

        mockMvc.perform(as("ADMIN", get(PATH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aiDailyQuota").value(20))
                .andExpect(jsonPath("$.isBannedFromRatedContests").value(false));
    }

    @Test
    @DisplayName("Sửa hạn chế trả 200 với trạng thái mới, người thực hiện lấy từ JWT")
    void patch_admin_returnsUpdated() throws Exception {
        when(adminUserRestrictionService.update(
                        eq("viet.luu"), any(UpdateRestrictionsRequest.class), eq(TestJwts.USER_ID)))
                .thenReturn(new UserRestrictionsResponse(5, true));

        mockMvc.perform(as("ADMIN", json(VALID_BODY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aiDailyQuota").value(5))
                .andExpect(jsonPath("$.isBannedFromRatedContests").value(true));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(
            delimiter = '|',
            value = {
                "{\"reason\":\"x\"}|aiDailyQuota|Cần ít nhất một thay đổi",
                "{\"aiDailyQuota\":-1,\"reason\":\"x\"}|aiDailyQuota|Hạn mức AI từ 0 đến 200",
                "{\"aiDailyQuota\":201,\"reason\":\"x\"}|aiDailyQuota|Hạn mức AI từ 0 đến 200",
                "{\"isBannedFromRatedContests\":true}|reason|Không được để trống",
                "{\"isBannedFromRatedContests\":true,\"reason\":\"  \"}|reason|Không được để trống"
            })
    @DisplayName("Dữ liệu sửa hạn chế sai: 400 kèm lỗi đúng trường, không sửa gì")
    void patch_invalidBody_returnsFieldError(String body, String field, String message)
            throws Exception {
        mockMvc.perform(as("ADMIN", json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value(field))
                .andExpect(jsonPath("$.errors[0].message").value(message));

        verifyNoInteractions(adminUserRestrictionService);
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(
            delimiter = '|',
            value = {
                "{\"aiDailyQuota\":1.5,\"reason\":\"x\"}|aiDailyQuota",
                "{\"aiDailyQuota\":\"5\",\"reason\":\"x\"}|aiDailyQuota",
                "{\"isBannedFromRatedContests\":\"yes\",\"reason\":\"x\"}|isBannedFromRatedContests"
            })
    @DisplayName("Sai kiểu dữ liệu (số thực, chuỗi): 400 trên đúng trường, không làm tròn")
    void patch_wrongType_returnsFieldError(String body, String field) throws Exception {
        mockMvc.perform(as("ADMIN", json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value(field));

        verifyNoInteractions(adminUserRestrictionService);
    }

    @Test
    @DisplayName("Admin khác vừa sửa cùng lúc (khoá lạc quan): 409 CONCURRENT_MODIFICATION")
    void patch_concurrentModification_returnsConflict() throws Exception {
        when(adminUserRestrictionService.update(
                        eq("viet.luu"), any(UpdateRestrictionsRequest.class), eq(TestJwts.USER_ID)))
                .thenThrow(
                        new ObjectOptimisticLockingFailureException(UserRestriction.class, "id"));

        mockMvc.perform(as("ADMIN", json(VALID_BODY)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONCURRENT_MODIFICATION"));
    }

    @Test
    @DisplayName("Lý do quá 500 ký tự: 400 trên trường reason")
    void patch_reasonTooLong_returnsFieldError() throws Exception {
        String body = "{\"aiDailyQuota\":5,\"reason\":\"" + "a".repeat(501) + "\"}";

        mockMvc.perform(as("ADMIN", json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("reason"));
    }

    @Test
    @DisplayName("Tự đặt hạn chế cho chính mình: 422 CANNOT_MODIFY_SELF")
    void patch_self_returnsUnprocessable() throws Exception {
        when(adminUserRestrictionService.update(
                        eq("viet.luu"), any(UpdateRestrictionsRequest.class), eq(TestJwts.USER_ID)))
                .thenThrow(
                        new BusinessRuleException(
                                ErrorCode.CANNOT_MODIFY_SELF,
                                "Bạn không thể tự đặt hạn chế cho chính mình"));

        mockMvc.perform(as("ADMIN", json(VALID_BODY)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CANNOT_MODIFY_SELF"));
    }

    @Test
    @DisplayName("Hạn chế: không phải ADMIN bị 403, chưa đăng nhập bị 401, cả xem lẫn sửa")
    void restrictions_requireAdminRole() throws Exception {
        for (String role : List.of("LEARNER", "SETTER", "MODERATOR")) {
            mockMvc.perform(as(role, get(PATH))).andExpect(status().isForbidden());
            mockMvc.perform(as(role, json(VALID_BODY))).andExpect(status().isForbidden());
        }
        mockMvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        mockMvc.perform(json(VALID_BODY)).andExpect(status().isUnauthorized());

        verifyNoInteractions(adminUserRestrictionService);
    }

    private static MockHttpServletRequestBuilder json(String body) {
        return patch(PATH).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private MockHttpServletRequestBuilder as(String role, MockHttpServletRequestBuilder request) {
        return request.header(
                HttpHeaders.AUTHORIZATION, "Bearer " + TestJwts.validToken(signingKey, role));
    }
}
