package com.sqljudge.problem.repository;

import com.sqljudge.problem.entity.ProblemStatus;

public interface StatusCountRow {

    ProblemStatus getStatus();

    long getTotal();
}
