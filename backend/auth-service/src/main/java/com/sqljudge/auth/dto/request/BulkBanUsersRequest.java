package com.sqljudge.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record BulkBanUsersRequest(
        @NotEmpty(message = "Chọn từ 1 đến 100 tài khoản") @Size(max = 100, message = "Chọn từ 1 đến 100 tài khoản") List<
                                @NotBlank(message = "Username không hợp lệ") @Size(max = 30, message = "Username không hợp lệ") String>
                        usernames,
        @NotNull(message = "Vui lòng chọn thời hạn khoá") BanDuration duration,
        @NotBlank(message = "Không được để trống") @Size(max = 500, message = "Tối đa 500 ký tự") String reason,
        @NotNull(message = "Phải là true hoặc false") Boolean notifyByEmail) {

    public BanUserRequest toBanRequest() {
        return new BanUserRequest(duration, reason, notifyByEmail);
    }
}
