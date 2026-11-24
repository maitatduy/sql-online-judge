package com.sqljudge.problem.repository;

import com.sqljudge.problem.entity.Difficulty;

public interface DifficultyCount {

    Difficulty getDifficulty();

    long getTotal();
}
