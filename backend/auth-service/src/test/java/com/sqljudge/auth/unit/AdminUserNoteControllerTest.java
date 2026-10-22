package com.sqljudge.auth.unit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.jwk.RSAKey;
import com.sqljudge.auth.config.JwtKeyConfig;
import com.sqljudge.auth.config.SecurityConfig;
import com.sqljudge.auth.controller.AdminUserNoteController;
import com.sqljudge.auth.dto.request.AddUserNoteRequest;
import com.sqljudge.auth.dto.response.UserNoteResponse;
import com.sqljudge.auth.dto.response.UserNotesResponse;
import com.sqljudge.auth.service.AdminUserNoteService;
import com.sqljudge.common.config.CommonAutoConfiguration;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.common.security.CurrentUser;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

@WebMvcTest(AdminUserNoteController.class)
@Import({SecurityConfig.class, JwtKeyConfig.class})
@ImportAutoConfiguration(CommonAutoConfiguration.class)
@TestPropertySource(
        properties = {
            "sqljudge.jwt.generate-ephemeral-key=true",
            "sqljudge.security.allowed-origins=http://localhost:3000"
        })
class AdminUserNoteControllerTest {

    private static final String NOTES_PATH = "/api/admin/users/viet.luu/notes";
    private static final UUID NOTE_ID = UUID.fromString("0192f3a0-0000-7000-8000-0000000000c1");
    private static final UserNoteResponse NOTE =
            new UserNoteResponse(
                    NOTE_ID, "minh.anh", "Đã nhắc nhở", Instant.parse("2026-10-21T03:00:00Z"));

    @Autowired private MockMvc mockMvc;
    @Autowired private RSAKey signingKey;

    @MockitoBean private AdminUserNoteService adminUserNoteService;

    @Test
    @DisplayName("Xem ghi chú trả 200 với danh sách items")
    void list_admin_returnsItems() throws Exception {
        when(adminUserNoteService.list("viet.luu"))
                .thenReturn(new UserNotesResponse(List.of(NOTE)));

        mockMvc.perform(as("ADMIN", get(NOTES_PATH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(NOTE_ID.toString()))
                .andExpect(jsonPath("$.items[0].authorUsername").value("minh.anh"))
                .andExpect(jsonPath("$.items[0].body").value("Đã nhắc nhở"))
                .andExpect(jsonPath("$.items[0].createdAt").value("2026-10-21T03:00:00Z"));
    }

    @Test
    @DisplayName("Thêm ghi chú trả 201 kèm Location và ghi chú vừa tạo, người viết lấy từ JWT")
    void add_admin_returnsCreated() throws Exception {
        when(adminUserNoteService.add(
                        eq("viet.luu"), any(AddUserNoteRequest.class), any(CurrentUser.class)))
                .thenReturn(NOTE);

        mockMvc.perform(as("ADMIN", json("{\"body\":\"Đã nhắc nhở\"}")))
                .andExpect(status().isCreated())
                .andExpect(
                        header().string(
                                        HttpHeaders.LOCATION,
                                        "/api/admin/users/viet.luu/notes/" + NOTE_ID))
                .andExpect(jsonPath("$.id").value(NOTE_ID.toString()))
                .andExpect(jsonPath("$.authorUsername").value("minh.anh"));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"{}", "{\"body\":\"\"}", "{\"body\":\"   \"}", "LONG"})
    @DisplayName("Nội dung ghi chú trống hoặc quá 1000 ký tự: 400 trên trường body")
    void add_invalidBody_returnsFieldError(String body) throws Exception {
        String json = "LONG".equals(body) ? "{\"body\":\"" + "a".repeat(1001) + "\"}" : body;

        mockMvc.perform(as("ADMIN", json(json)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("body"));

        verifyNoInteractions(adminUserNoteService);
    }

    @Test
    @DisplayName("Nội dung đúng 1000 ký tự sau khi bỏ khoảng trắng hai đầu vẫn hợp lệ")
    void add_bodyOfThousandCharsAfterStrip_isAccepted() throws Exception {
        when(adminUserNoteService.add(
                        eq("viet.luu"), any(AddUserNoteRequest.class), any(CurrentUser.class)))
                .thenReturn(NOTE);

        mockMvc.perform(as("ADMIN", json("{\"body\":\"  " + "a".repeat(1000) + "  \"}")))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Thêm ghi chú cho tài khoản không có hoặc đã xoá: 404 USER_NOT_FOUND")
    void add_missingUser_returnsNotFound() throws Exception {
        when(adminUserNoteService.add(
                        eq("viet.luu"), any(AddUserNoteRequest.class), any(CurrentUser.class)))
                .thenThrow(
                        new NotFoundException(
                                ErrorCode.USER_NOT_FOUND, "Không tìm thấy người dùng"));

        mockMvc.perform(as("ADMIN", json("{\"body\":\"Ghi chú\"}")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    @DisplayName("Xem một ghi chú theo id trả 200 với ghi chú đó")
    void getOne_admin_returnsNote() throws Exception {
        when(adminUserNoteService.get("viet.luu", NOTE_ID)).thenReturn(NOTE);

        mockMvc.perform(as("ADMIN", get(NOTES_PATH + "/" + NOTE_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(NOTE_ID.toString()))
                .andExpect(jsonPath("$.body").value("Đã nhắc nhở"));
    }

    @Test
    @DisplayName("Ghi chú không thuộc tài khoản này: 404 NOTE_NOT_FOUND")
    void getOne_noteOfAnotherUser_returnsNotFound() throws Exception {
        when(adminUserNoteService.get("viet.luu", NOTE_ID))
                .thenThrow(
                        new NotFoundException(ErrorCode.NOTE_NOT_FOUND, "Không tìm thấy ghi chú"));

        mockMvc.perform(as("ADMIN", get(NOTES_PATH + "/" + NOTE_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOTE_NOT_FOUND"));
    }

    @Test
    @DisplayName("Ghi chú: không phải ADMIN bị 403, chưa đăng nhập bị 401, cả xem lẫn thêm")
    void notes_requireAdminRole() throws Exception {
        String notePath = NOTES_PATH + "/" + NOTE_ID;
        for (String role : List.of("LEARNER", "SETTER", "MODERATOR")) {
            mockMvc.perform(as(role, get(NOTES_PATH))).andExpect(status().isForbidden());
            mockMvc.perform(as(role, get(notePath))).andExpect(status().isForbidden());
            mockMvc.perform(as(role, json("{\"body\":\"x\"}"))).andExpect(status().isForbidden());
        }
        mockMvc.perform(get(NOTES_PATH)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(notePath)).andExpect(status().isUnauthorized());
        mockMvc.perform(json("{\"body\":\"x\"}")).andExpect(status().isUnauthorized());

        verifyNoInteractions(adminUserNoteService);
    }

    private static MockHttpServletRequestBuilder json(String body) {
        return post(NOTES_PATH).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private MockHttpServletRequestBuilder as(String role, MockHttpServletRequestBuilder request) {
        return request.header(
                HttpHeaders.AUTHORIZATION, "Bearer " + TestJwts.validToken(signingKey, role));
    }
}
