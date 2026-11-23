package com.sqljudge.community.dto.request;

import com.sqljudge.community.entity.ReportAction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResolveReportRequest(
        @NotNull(message = "Chọn cách xử lý") ReportAction action,
        @Size(max = 1000, message = "Tối đa 1.000 ký tự") String note) {}
