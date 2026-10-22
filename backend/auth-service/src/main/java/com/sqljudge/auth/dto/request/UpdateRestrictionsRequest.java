package com.sqljudge.auth.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sqljudge.auth.validation.StrictIntegerDeserializer;
import com.sqljudge.auth.validation.ValidRestrictionsUpdate;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@ValidRestrictionsUpdate
public record UpdateRestrictionsRequest(
        @JsonDeserialize(using = StrictIntegerDeserializer.class)
                @Min(value = 0, message = "Hạn mức AI từ 0 đến 200") @Max(value = 200, message = "Hạn mức AI từ 0 đến 200") Integer aiDailyQuota,
        Boolean isBannedFromRatedContests,
        @NotBlank(message = "Không được để trống") @Size(max = 500, message = "Tối đa 500 ký tự") String reason) {

    public boolean hasChange() {
        return aiDailyQuota != null || isBannedFromRatedContests != null;
    }
}
