import type { Dialect } from "@/lib/dialect";

const DIALECTS: readonly Dialect[] = ["POSTGRESQL", "MYSQL", "SQLITE"];

export function draftKeyOf(problemId: string, dialect: Dialect): string {
    return `draft:${problemId}:${dialect}`;
}

export function dialectChoiceKeyOf(problemId: string): string {
    return `dialect:${problemId}`;
}

function read(key: string): string | null {
    try {
        return localStorage.getItem(key);
    } catch {
        return null;
    }
}

export function readWorkspaceCode(problemId: string): string | null {
    const chosen = DIALECTS.find((dialect) => dialect === read(dialectChoiceKeyOf(problemId)));
    const candidates = chosen ? [chosen] : DIALECTS;
    for (const dialect of candidates) {
        const code = read(draftKeyOf(problemId, dialect));
        if (code !== null && code.trim() !== "") {
            return code;
        }
    }
    return null;
}
