package com.sqljudge.problem.dto.response;

public record AdminProblemBankSummaryResponse(
        long publicCount, long contestOnlyCount, long hiddenCount, long lowAcceptanceCount) {}
