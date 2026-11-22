package com.sqljudge.contest.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sqljudge.contest.entity.ContestRegistration;
import java.time.Instant;

public record RegistrationResponse(
        @JsonProperty("isRegistered") boolean isRegistered, Instant registeredAt) {

    public static RegistrationResponse of(ContestRegistration registration) {
        return new RegistrationResponse(true, registration.getCreatedAt());
    }

    public static RegistrationResponse none() {
        return new RegistrationResponse(false, null);
    }
}
