package com.sqljudge.contest.controller;

import com.sqljudge.contest.dto.response.RegistrationResponse;
import com.sqljudge.contest.service.ContestRegistrationService;
import com.sqljudge.contest.service.ContestRegistrationService.RegistrationOutcome;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contests/{slug}/registration")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ContestRegistrationController {

    private final ContestRegistrationService registrationService;

    @GetMapping
    @Operation(summary = "Người đang đăng nhập đã đăng ký contest chưa")
    @ApiResponse(responseCode = "200", description = "Trạng thái đăng ký")
    @ApiResponse(responseCode = "404", description = "Không có contest")
    public RegistrationResponse status(@PathVariable String slug) {
        return registrationService.currentStatus(slug);
    }

    @PostMapping
    @Operation(summary = "Đăng ký contest, cho phép đăng ký muộn khi contest đang diễn ra")
    @ApiResponse(responseCode = "201", description = "Đã đăng ký")
    @ApiResponse(responseCode = "200", description = "Đã đăng ký từ trước")
    @ApiResponse(responseCode = "403", description = "Tài khoản bị cấm thi contest tính rating")
    @ApiResponse(responseCode = "404", description = "Không có contest")
    @ApiResponse(responseCode = "422", description = "Contest đã kết thúc")
    @ApiResponse(responseCode = "503", description = "Không gọi được auth-service")
    public ResponseEntity<RegistrationResponse> register(@PathVariable String slug) {
        RegistrationOutcome outcome = registrationService.register(slug);
        return ResponseEntity.status(outcome.isCreated() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(outcome.registration());
    }

    @DeleteMapping
    @Operation(summary = "Huỷ đăng ký trước khi contest bắt đầu")
    @ApiResponse(responseCode = "204", description = "Đã huỷ hoặc chưa từng đăng ký")
    @ApiResponse(responseCode = "409", description = "Contest đã bắt đầu")
    public ResponseEntity<Void> unregister(@PathVariable String slug) {
        registrationService.unregister(slug);
        return ResponseEntity.noContent().build();
    }
}
