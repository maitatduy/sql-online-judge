package com.sqljudge.contest.client;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;

public record ContestStatement(
        String title,
        String difficulty,
        String statementMarkdown,
        List<String> dialects,
        Map<String, String> engineVersions,
        String starterCode,
        List<JsonNode> schema,
        List<JsonNode> examples,
        boolean requiresOrder,
        List<String> allowedStatements,
        int timeLimitMs,
        int hiddenTestcaseCount) {}
