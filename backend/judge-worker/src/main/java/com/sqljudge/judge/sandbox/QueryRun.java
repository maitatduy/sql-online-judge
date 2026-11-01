package com.sqljudge.judge.sandbox;

public record QueryRun(DatasetSnapshot dataset, String sql, RunLimits limits) {}
