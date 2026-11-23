package com.sqljudge.problem.service;

import com.sqljudge.problem.entity.Difficulty;
import com.sqljudge.problem.entity.ProblemStatus;
import com.sqljudge.problem.entity.ProblemVisibility;

public record AdminProblemSearch(
        ProblemStatus status,
        ProblemVisibility visibility,
        Difficulty difficulty,
        String tag,
        String keyword,
        String sort) {}
