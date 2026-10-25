package com.sqljudge.auth.controller;

import com.sqljudge.auth.dto.response.AuditActorsResponse;
import com.sqljudge.auth.dto.response.AuditLogEntryResponse;
import com.sqljudge.auth.entity.AdminActionType;
import com.sqljudge.auth.entity.AuditCategory;
import com.sqljudge.auth.service.AdminAuditLogService;
import com.sqljudge.auth.service.AuditLogCsv;
import com.sqljudge.auth.service.AuditLogSearch;
import com.sqljudge.common.security.SecurityUtils;
import com.sqljudge.common.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/admin/audit-log")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminAuditLogController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_KEYWORD_LENGTH = 100;

    private static final MediaType TEXT_CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final AdminAuditLogService adminAuditLogService;
    private final Clock clock;

    @Operation(
            summary = "Xuất nhật ký theo bộ lọc hiện tại ra file CSV tải về ngay",
            description =
                    "Cùng bộ lọc với danh sách, không phân trang. Thời gian theo UTC. Mỗi lần xuất"
                            + " được ghi nhật ký EXPORT_AUDIT_LOG kèm bộ lọc. Giá trị bắt đầu bằng"
                            + " = + - @, tab hoặc CR được thêm tiền tố ' để chống CSV injection.")
    @ApiResponse(responseCode = "200", description = "File CSV, tên audit-log-yyyyMMdd-HHmm.csv")
    @ApiResponse(responseCode = "400", description = "Tham số lọc không hợp lệ")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(responseCode = "403", description = "Không phải ADMIN")
    @GetMapping("/export")
    public ResponseEntity<StreamingResponseBody> exportAuditLog(
            @RequestParam(required = false) String user,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) AdminActionType action,
            @RequestParam(required = false) AuditCategory category,
            @RequestParam(required = false)
                    @Size(max = MAX_KEYWORD_LENGTH, message = "Từ khoá quá dài") String q,
            @RequestParam(required = false) Instant from) {
        AuditLogSearch search = new AuditLogSearch(user, actor, action, category, q, from);
        adminAuditLogService.recordExport(search, SecurityUtils.requireCurrentUser().id());
        return ResponseEntity.ok()
                .contentType(TEXT_CSV)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(AuditLogCsv.fileName(clock.instant()))
                                .build()
                                .toString())
                .body(out -> adminAuditLogService.writeCsv(search, out));
    }

    @Operation(
            summary =
                    "Nhật ký thao tác của quản trị viên: tìm kiếm, lọc theo nhóm, đối tượng,"
                            + " người, loại và thời gian")
    @ApiResponse(responseCode = "200", description = "Một trang nhật ký, mới nhất đứng trước")
    @ApiResponse(responseCode = "400", description = "Tham số lọc hoặc phân trang không hợp lệ")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(responseCode = "403", description = "Không phải ADMIN")
    @GetMapping
    public PageResponse<AuditLogEntryResponse> listAuditLog(
            @RequestParam(required = false) String user,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) AdminActionType action,
            @RequestParam(required = false) AuditCategory category,
            @RequestParam(required = false)
                    @Size(max = MAX_KEYWORD_LENGTH, message = "Từ khoá quá dài") String q,
            @RequestParam(required = false) Instant from,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "Phải từ 1 trở lên") int page,
            @RequestParam(defaultValue = "20")
                    @Min(value = 1, message = "Phải từ 1 đến 100") @Max(value = MAX_PAGE_SIZE, message = "Phải từ 1 đến 100") int size) {
        return adminAuditLogService.search(
                new AuditLogSearch(user, actor, action, category, q, from), page, size);
    }

    @Operation(summary = "Những người đã có thao tác trong nhật ký, để chọn khi lọc")
    @ApiResponse(responseCode = "200", description = "Username theo thứ tự chữ cái")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(responseCode = "403", description = "Không phải ADMIN")
    @GetMapping("/actors")
    public AuditActorsResponse listActors() {
        return adminAuditLogService.listActors();
    }
}
