package com.sqljudge.problem.controller;

import com.sqljudge.problem.dto.response.InternalDatasetVariantResponse;
import com.sqljudge.problem.service.InternalDatasetVariantService;
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
@RequestMapping("/internal/dataset-variants")
@PreAuthorize("hasRole('INTERNAL_SERVICE')")
@RequiredArgsConstructor
public class InternalDatasetVariantController {

    private final InternalDatasetVariantService internalDatasetVariantService;

    @GetMapping("/{variantId}")
    @Operation(summary = "Thông tin một bộ dữ liệu để gắn vào nơi khác, không kèm dữ liệu")
    @ApiResponse(responseCode = "200", description = "Bản, checksum, cờ công khai và đã chốt")
    @ApiResponse(responseCode = "401", description = "Thiếu hoặc sai token nội bộ")
    @ApiResponse(responseCode = "404", description = "Không có bộ dữ liệu này")
    public InternalDatasetVariantResponse describe(@PathVariable UUID variantId) {
        return internalDatasetVariantService.describe(variantId);
    }
}
