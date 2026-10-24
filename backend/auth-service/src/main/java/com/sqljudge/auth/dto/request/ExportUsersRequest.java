package com.sqljudge.auth.dto.request;

import com.sqljudge.auth.validation.ValidUserExport;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@ValidUserExport
public record ExportUsersRequest(
        @NotNull(message = "Phạm vi xuất không hợp lệ") ExportScope scope,
        @Valid UserExportFilters filters,
        List<@Size(max = 30, message = "Username không hợp lệ") String> usernames,
        @NotNull(message = "Phải là true hoặc false") Boolean includeEmail,
        @Size(max = 500, message = "Tối đa 500 ký tự") String reason) {

    public boolean isEmailIncluded() {
        return Boolean.TRUE.equals(includeEmail);
    }
}
