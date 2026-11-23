package com.sqljudge.problem.repository;

import com.sqljudge.problem.entity.ProblemVisibility;

public interface VisibilityCountRow {

    ProblemVisibility getVisibility();

    long getTotal();
}
