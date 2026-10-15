package com.sqljudge.auth.controller;

import com.sqljudge.auth.dto.response.UsernameAvailabilityResponse;
import com.sqljudge.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private static final String USERNAME_PATTERN = "^[a-z0-9._]{3,30}$";
    private static final String USERNAME_MESSAGE =
            "Tên người dùng gồm 3-30 ký tự: chữ thường, số, dấu chấm hoặc gạch dưới";

    private final UserService userService;

    @Operation(summary = "Kiểm tra username còn trống")
    @ApiResponse(responseCode = "200", description = "Kết quả kiểm tra")
    @ApiResponse(responseCode = "400", description = "Username sai định dạng")
    @GetMapping("/availability")
    public UsernameAvailabilityResponse checkUsernameAvailability(
            @RequestParam @Pattern(regexp = USERNAME_PATTERN, message = USERNAME_MESSAGE) String username) {
        return userService.checkUsernameAvailability(username);
    }
}
