package com.sqljudge.problem.dto.request;

import com.sqljudge.problem.entity.LogicalType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateDatasetSchemaRequest(
        @NotNull(message = "Không được để trống") @Size(min = 1, max = 10, message = "Cần từ 1 đến 10 bảng") List<@Valid @NotNull(message = "Không được để trống") SchemaTableRequest> tables) {

    private static final String IDENTIFIER = "[a-z_][a-z0-9_]*";
    private static final String IDENTIFIER_MESSAGE =
            "Chỉ gồm chữ thường không dấu, số và dấu gạch dưới, không bắt đầu bằng số";

    public record SchemaTableRequest(
            @NotBlank(message = "Không được để trống") @Pattern(regexp = IDENTIFIER, message = IDENTIFIER_MESSAGE) @Size(max = 64, message = "Tối đa 64 ký tự") String name,
            @NotNull(message = "Không được để trống") @Size(min = 1, max = 30, message = "Cần từ 1 đến 30 cột") List<@Valid @NotNull(message = "Không được để trống") SchemaColumnRequest>
                            columns) {}

    public record SchemaColumnRequest(
            @NotBlank(message = "Không được để trống") @Pattern(regexp = IDENTIFIER, message = IDENTIFIER_MESSAGE) @Size(max = 64, message = "Tối đa 64 ký tự") String name,
            @NotNull(message = "Không được để trống") LogicalType type,
            boolean isPrimaryKey,
            boolean isNullable,
            @Valid ColumnReferenceRequest references) {}

    public record ColumnReferenceRequest(
            @NotBlank(message = "Không được để trống") String table,
            @NotBlank(message = "Không được để trống") String column) {}
}
