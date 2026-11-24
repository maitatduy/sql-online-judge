package com.sqljudge.auth.controller;

import com.sqljudge.auth.dto.response.PublicProfileResponse;
import com.sqljudge.auth.dto.response.UsernameAvailabilityResponse;
import com.sqljudge.auth.service.PublicProfileService;
import com.sqljudge.auth.service.UserService;
import com.sqljudge.auth.validation.UsernameRules;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PublicProfileService publicProfileService;

    @Operation(summary = "Kiểm tra username còn trống")
    @ApiResponse(responseCode = "200", description = "Kết quả kiểm tra")
    @ApiResponse(responseCode = "400", description = "Username sai định dạng")
    @GetMapping("/availability")
    public UsernameAvailabilityResponse checkUsernameAvailability(
            @RequestParam
                    @Pattern(
                            regexp = UsernameRules.PATTERN,
                            message = UsernameRules.PATTERN_MESSAGE)
                    String username) {
        return userService.checkUsernameAvailability(username);
    }

    @GetMapping("/{username}")
    @Operation(summary = "Hồ sơ công khai của một người dùng")
    @ApiResponse(responseCode = "200", description = "Hồ sơ công khai")
    @ApiResponse(
            responseCode = "404",
            description = "Không có người dùng, đã xoá hoặc chưa xác minh")
    public PublicProfileResponse profile(@PathVariable String username) {
        return publicProfileService.profile(username);
    }
}
