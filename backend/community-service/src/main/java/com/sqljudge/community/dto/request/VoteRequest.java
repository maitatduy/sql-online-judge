package com.sqljudge.community.dto.request;

import com.sqljudge.community.entity.VoteTarget;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record VoteRequest(
        @NotNull(message = "Chọn đối tượng") VoteTarget targetType,
        @NotNull(message = "Chọn đối tượng") UUID targetId,
        @Min(value = -1, message = "Chỉ nhận -1, 0 hoặc 1") @Max(value = 1, message = "Chỉ nhận -1, 0 hoặc 1") int value) {}
