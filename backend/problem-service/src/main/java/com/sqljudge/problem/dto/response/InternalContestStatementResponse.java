package com.sqljudge.problem.dto.response;

import com.sqljudge.problem.dto.response.ProblemDetailResponse.ResultSetResponse;
import com.sqljudge.problem.dto.response.ProblemDetailResponse.SchemaTableResponse;
import com.sqljudge.problem.entity.Dialect;
import com.sqljudge.problem.entity.Difficulty;
import com.sqljudge.problem.entity.StatementType;
import java.util.List;
import java.util.Map;

public record InternalContestStatementResponse(
        String title,
        Difficulty difficulty,
        String statementMarkdown,
        List<Dialect> dialects,
        Map<Dialect, String> engineVersions,
        String starterCode,
        List<SchemaTableResponse> schema,
        List<ResultSetResponse> examples,
        boolean requiresOrder,
        List<StatementType> allowedStatements,
        int timeLimitMs,
        int hiddenTestcaseCount) {}
