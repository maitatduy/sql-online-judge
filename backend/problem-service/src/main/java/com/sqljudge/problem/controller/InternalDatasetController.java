package com.sqljudge.problem.controller;

import com.sqljudge.problem.dto.response.InternalDatasetSnapshotResponse;
import com.sqljudge.problem.service.InternalDatasetSnapshotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/dataset-versions")
@PreAuthorize("hasRole('INTERNAL_SERVICE')")
@RequiredArgsConstructor
public class InternalDatasetController {

    private final InternalDatasetSnapshotService internalDatasetSnapshotService;

    @GetMapping("/{versionId}/snapshot")
    @Operation(summary = "Snapshot bản database đã chốt cho máy chấm: schema và dữ liệu đã parse")
    @ApiResponse(responseCode = "200", description = "Snapshot gồm cả bộ dữ liệu ẩn")
    @ApiResponse(responseCode = "401", description = "Thiếu hoặc sai token nội bộ")
    @ApiResponse(responseCode = "404", description = "Không có bản này hoặc bản chưa chốt")
    public InternalDatasetSnapshotResponse snapshot(@PathVariable UUID versionId) {
        return internalDatasetSnapshotService.snapshot(versionId);
    }
}
