package com.sqljudge.learning.dto.response;

import com.sqljudge.learning.entity.Dialect;
import com.sqljudge.learning.entity.LessonCell;
import java.util.UUID;

public record StudioCellResponse(
        UUID id, int position, Dialect dialect, String starterSql, UUID datasetVariantId) {

    public static StudioCellResponse of(LessonCell cell) {
        return new StudioCellResponse(
                cell.getId(),
                cell.getPosition(),
                cell.getDialect(),
                cell.getStarterSql(),
                cell.getDatasetVariantId());
    }
}
