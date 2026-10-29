package com.sqljudge.problem.service;

import com.sqljudge.problem.dto.response.ProblemProgress;
import com.sqljudge.problem.entity.Difficulty;

public record ProblemSearch(
        String keyword, Difficulty difficulty, String tag, ProblemProgress progress) {}
