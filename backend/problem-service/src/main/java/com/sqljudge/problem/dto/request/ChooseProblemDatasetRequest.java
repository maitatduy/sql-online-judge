package com.sqljudge.problem.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ChooseProblemDatasetRequest(
        @NotNull(message = "Chưa chọn database") UUID datasetId) {}
