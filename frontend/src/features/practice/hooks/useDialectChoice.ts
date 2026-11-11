import { useState } from "react";

import type { Dialect } from "@/lib/dialect";

function choiceKeyOf(problemId: string): string {
    return `dialect:${problemId}`;
}

function readChoice(problemId: string, dialects: Dialect[]): Dialect | null {
    try {
        const stored = localStorage.getItem(choiceKeyOf(problemId));
        return dialects.find((dialect) => dialect === stored) ?? null;
    } catch {
        return null;
    }
}

function writeChoice(problemId: string, dialect: Dialect): void {
    try {
        localStorage.setItem(choiceKeyOf(problemId), dialect);
    } catch {
        return;
    }
}

export function useDialectChoice(problemId: string, dialects: Dialect[]) {
    const [choice, setChoice] = useState(() => ({
        problemId,
        dialect: readChoice(problemId, dialects) ?? dialects[0],
    }));

    if (choice.problemId !== problemId) {
        setChoice({ problemId, dialect: readChoice(problemId, dialects) ?? dialects[0] });
    }

    function chooseDialect(dialect: Dialect) {
        writeChoice(problemId, dialect);
        setChoice({ problemId, dialect });
    }

    return { dialect: choice.dialect, chooseDialect };
}
