package com.sqljudge.problem.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sqljudge.problem.entity.Difficulty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record BulkUpdateProblemsRequest(
        @NotEmpty(message = "Chọn ít nhất một bài") @Size(max = 100, message = "Tối đa 100 bài") List<UUID> problemIds,
        @Size(max = 10, message = "Tối đa 10 chủ đề") List<String> addTagSlugs,
        Difficulty difficulty,
        boolean hide) {

    @JsonIgnore
    @AssertTrue(message = "Chọn một thao tác") public boolean isAnyChange() {
        return hasTags() || difficulty != null || hide;
    }

    public boolean hasTags() {
        return addTagSlugs != null && !addTagSlugs.isEmpty();
    }
}
