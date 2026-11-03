package com.sqljudge.problem.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.sqljudge.problem.dto.response.DialectCheckResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AnswerTestcaseCheckedEvent(
        UUID eventId,
        Instant occurredAt,
        int version,
        UUID checkId,
        UUID testcaseId,
        int position,
        int testcaseCount,
        String systemError,
        ExpectedTable expected,
        List<DialectCheckResult> dialects) {

    public AnswerTestcaseCheckedEvent {
        dialects = dialects == null ? List.of() : List.copyOf(dialects);
    }

    public record ExpectedTable(List<String> columns, JsonNode rows) {}
}
