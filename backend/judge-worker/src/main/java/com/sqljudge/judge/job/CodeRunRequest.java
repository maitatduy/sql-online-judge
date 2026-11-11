package com.sqljudge.judge.job;

import com.sqljudge.judge.sandbox.Dialect;
import java.util.UUID;

public record CodeRunRequest(UUID runId, Dialect dialect, String code, JudgePackage judgePackage) {}
