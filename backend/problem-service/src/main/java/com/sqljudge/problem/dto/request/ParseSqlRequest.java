package com.sqljudge.problem.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ParseSqlRequest(
        @NotNull(message = "Không được để trống") @Size(max = 20_971_520, message = "File SQL tối đa 20 MB") String sql) {}
