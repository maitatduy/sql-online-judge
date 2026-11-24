package com.sqljudge.notification.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record UpdatePreferenceRequest(
        @JsonProperty("isEmailEnabled") @NotNull(message = "Chọn bật hoặc tắt") Boolean isEmailEnabled) {}
