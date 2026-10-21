package com.sqljudge.auth.controller;

import com.sqljudge.auth.dto.request.AddUserNoteRequest;
import com.sqljudge.auth.dto.response.UserNoteResponse;
import com.sqljudge.auth.dto.response.UserNotesResponse;
import com.sqljudge.auth.service.AdminUserNoteService;
import com.sqljudge.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/admin/users/{username}/notes")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserNoteController {

    private final AdminUserNoteService adminUserNoteService;

    @Operation(summary = "Ghi chú nội bộ của admin về một tài khoản, mới nhất trước, tối đa 200")
    @ApiResponse(responseCode = "200", description = "Danh sách ghi chú, kể cả tài khoản đã xoá")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(responseCode = "403", description = "Không phải ADMIN")
    @ApiResponse(responseCode = "404", description = "Không có người dùng với username này")
    @GetMapping
    public UserNotesResponse list(@PathVariable String username) {
        return adminUserNoteService.list(username);
    }

    @Operation(summary = "Thêm ghi chú nội bộ, không sửa hay xoá được sau khi tạo")
    @ApiResponse(responseCode = "201", description = "Ghi chú vừa tạo")
    @ApiResponse(responseCode = "400", description = "Nội dung trống hoặc quá 1000 ký tự")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(responseCode = "403", description = "Không phải ADMIN")
    @ApiResponse(responseCode = "404", description = "Không có tài khoản hoặc đã bị xoá")
    @PostMapping
    public ResponseEntity<UserNoteResponse> add(
            @PathVariable String username, @Valid @RequestBody AddUserNoteRequest request) {
        UserNoteResponse note =
                adminUserNoteService.add(username, request, SecurityUtils.requireCurrentUser());
        URI location =
                UriComponentsBuilder.fromPath("/api/admin/users/{username}/notes/{id}")
                        .buildAndExpand(username, note.id())
                        .toUri();
        return ResponseEntity.created(location).body(note);
    }
}
