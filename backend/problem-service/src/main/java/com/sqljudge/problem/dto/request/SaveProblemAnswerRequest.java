package com.sqljudge.problem.dto.request;

import com.sqljudge.problem.entity.Dialect;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record SaveProblemAnswerRequest(
        @NotNull(message = "Không được để trống") @Size(min = 1, max = 3, message = "Cần từ 1 đến 3 đáp án") List<@Valid @NotNull(message = "Không được để trống") SolutionRequest> solutions,
        @NotNull(message = "Không được để trống") @Size(min = 1, max = 20, message = "Cần từ 1 đến 20 testcase") List<@NotNull(message = "Không được để trống") UUID> testcaseVariantIds,
        boolean requiresOrder,
        boolean compareColumnNames,
        @DecimalMin(value = "0", message = "Từ 0 đến 1") @DecimalMax(value = "1", message = "Từ 0 đến 1") double floatTolerance) {

    public record SolutionRequest(
            @NotNull(message = "Chưa chọn dialect") Dialect dialect,
            @NotBlank(message = "Không được để trống") @Size(max = 65_536, message = "Đáp án tối đa 64 KB") String sql) {}
}
