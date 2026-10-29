package com.sqljudge.problem.repository;

import java.util.UUID;

public record ProblemTagRow(UUID problemId, String slug, String name) {}
