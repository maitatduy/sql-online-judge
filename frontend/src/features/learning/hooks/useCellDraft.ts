import { useState } from "react";

const DRAFT_KEY_PREFIX = "sqljudge:lesson-cell:";

function readDraft(cellKey: string): string | null {
    try {
        return localStorage.getItem(DRAFT_KEY_PREFIX + cellKey);
    } catch {
        return null;
    }
}

function writeDraft(cellKey: string, sql: string | null): void {
    try {
        if (sql === null) {
            localStorage.removeItem(DRAFT_KEY_PREFIX + cellKey);
        } else {
            localStorage.setItem(DRAFT_KEY_PREFIX + cellKey, sql);
        }
    } catch {
        return;
    }
}

export function useCellDraft(cellKey: string, starterSql: string) {
    const [sql, setSql] = useState(() => readDraft(cellKey) ?? starterSql);

    function changeSql(nextSql: string) {
        setSql(nextSql);
        writeDraft(cellKey, nextSql === starterSql ? null : nextSql);
    }

    return {
        sql,
        isChanged: sql !== starterSql,
        changeSql,
        reset: () => changeSql(starterSql),
    };
}
