package com.sqljudge.learning.dto.response;

import com.sqljudge.learning.entity.Dialect;
import java.util.UUID;

public record LessonCellResponse(UUID cellKey, String starterSql, Dialect dialect) {}
