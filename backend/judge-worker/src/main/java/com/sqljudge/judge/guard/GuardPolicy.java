package com.sqljudge.judge.guard;

import java.util.EnumSet;
import java.util.Set;

public record GuardPolicy(Set<StatementType> allowedStatements, int maxStatements) {

    private static final Set<StatementType> QUERY_STATEMENTS =
            EnumSet.of(StatementType.SELECT, StatementType.WITH);
    private static final int MAX_QUERY_STATEMENTS = 1;
    private static final int MAX_MODIFYING_STATEMENTS = 20;

    public GuardPolicy {
        allowedStatements = Set.copyOf(allowedStatements);
    }

    public static GuardPolicy of(Set<StatementType> allowedStatements) {
        int maxStatements =
                QUERY_STATEMENTS.containsAll(allowedStatements)
                        ? MAX_QUERY_STATEMENTS
                        : MAX_MODIFYING_STATEMENTS;
        return new GuardPolicy(allowedStatements, maxStatements);
    }

    public static GuardPolicy selectOnly() {
        return of(QUERY_STATEMENTS);
    }
}
