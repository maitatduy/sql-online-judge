package com.sqljudge.problem.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UploadVariantCsvRequest(
        @NotNull(message = "Không được để trống") @Size(max = 20_971_520, message = "CSV tối đa 20 MB") String csv) {}
