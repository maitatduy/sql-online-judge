package com.sqljudge.judge.sandbox;

import com.sqljudge.judge.compare.ExecutionOutcome;

public interface Sandbox {

    ExecutionOutcome run(QueryRun run);
}
