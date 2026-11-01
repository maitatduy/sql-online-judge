package com.sqljudge.problem.dto.request;

import com.sqljudge.problem.entity.Difficulty;
import com.sqljudge.problem.entity.ProblemStatus;

public record StudioProblemQuery(
        ProblemStatus status, Difficulty difficulty, String q, String sort, int page, int size) {}
