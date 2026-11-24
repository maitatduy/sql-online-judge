package com.sqljudge.notification.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PreferenceResponse(@JsonProperty("isEmailEnabled") boolean isEmailEnabled) {}
