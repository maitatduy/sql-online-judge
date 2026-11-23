package com.sqljudge.community.dto.request;

import com.sqljudge.community.entity.ReportReason;
import com.sqljudge.community.entity.ReportTarget;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateReportRequest(
        @NotNull(message = "Chọn nội dung báo cáo") ReportTarget targetType,
        @NotNull(message = "Chọn nội dung báo cáo") UUID targetId,
        @NotNull(message = "Chọn lý do") ReportReason reason,
        @Size(max = 500, message = "Tối đa 500 ký tự") String detail) {}
