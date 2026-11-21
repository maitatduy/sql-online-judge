package com.sqljudge.contest.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sqljudge.contest.entity.ContestSchedule;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateContestRequest(
        @NotBlank(message = "Không được để trống") @Size(max = 160, message = "Tối đa 160 ký tự") @Pattern(
                        regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
                        message = "Chỉ gồm chữ thường không dấu, số và dấu gạch nối")
                String slug,
        @NotBlank(message = "Không được để trống") @Size(max = 200, message = "Tối đa 200 ký tự") String title,
        @Size(max = 20000, message = "Tối đa 20.000 ký tự") String description,
        @NotNull(message = "Không được để trống") Instant startAt,
        @NotNull(message = "Không được để trống") @Min(value = 30, message = "Từ 30 đến 600 phút") @Max(value = 600, message = "Từ 30 đến 600 phút") Integer durationMinutes,
        @NotNull(message = "Không được để trống") @Min(value = 0, message = "Không được âm") Integer freezeMinutes,
        @NotNull(message = "Không được để trống") @Min(value = 0, message = "Từ 0 đến 60 phút") @Max(value = 60, message = "Từ 0 đến 60 phút") Integer penaltyMinutes,
        @JsonProperty("isRated") @NotNull(message = "Không được để trống") Boolean isRated) {

    public ContestSchedule toSchedule() {
        return new ContestSchedule(
                slug,
                title.strip(),
                description == null ? "" : description,
                startAt,
                durationMinutes,
                freezeMinutes,
                penaltyMinutes,
                isRated);
    }
}
