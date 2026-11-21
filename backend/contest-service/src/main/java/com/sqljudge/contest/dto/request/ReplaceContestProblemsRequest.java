package com.sqljudge.contest.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record ReplaceContestProblemsRequest(
        @NotNull(message = "Không được để trống") @Size(max = 26, message = "Tối đa 26 bài") List<@NotNull(message = "Không được để trống") UUID> problemIds) {

    public boolean hasDuplicates() {
        return problemIds.stream().distinct().count() != problemIds.size();
    }
}
