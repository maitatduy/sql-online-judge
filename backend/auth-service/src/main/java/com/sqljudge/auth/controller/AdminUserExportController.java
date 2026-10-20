package com.sqljudge.auth.controller;

import com.sqljudge.auth.dto.request.ExportUsersRequest;
import com.sqljudge.auth.service.AdminUserExportService;
import com.sqljudge.auth.service.UserExport;
import com.sqljudge.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserExportController {

    private static final MediaType TEXT_CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final AdminUserExportService adminUserExportService;
    private final Clock clock;

    @Operation(
            summary = "Xuất danh sách người dùng ra file CSV tải về ngay",
            description =
                    "Cột: username, email (chỉ khi includeEmail), role, status, joinedAt (UTC)."
                            + " Xuất kèm email bắt buộc có lý do và được ghi nhật ký. Giá trị bắt"
                            + " đầu bằng = + - @, tab hoặc CR được thêm tiền tố ' để chống CSV"
                            + " injection.")
    @ApiResponse(responseCode = "200", description = "File CSV, tên users-yyyyMMdd-HHmm.csv (UTC)")
    @ApiResponse(responseCode = "400", description = "Phạm vi, bộ lọc, danh sách hoặc lý do sai")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(responseCode = "403", description = "Không phải ADMIN")
    @PostMapping("/user-exports")
    public ResponseEntity<StreamingResponseBody> export(
            @Valid @RequestBody ExportUsersRequest request) {
        UserExport export =
                adminUserExportService.prepare(
                        request, SecurityUtils.requireCurrentUser().id(), clock.instant());
        return ResponseEntity.ok()
                .contentType(TEXT_CSV)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(export.fileName())
                                .build()
                                .toString())
                .body(out -> adminUserExportService.write(export, out));
    }
}
