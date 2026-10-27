import { useEffect, useRef, useState } from "react";

export const DRAFT_SAVE_DELAY_MS = 500;

const DRAFT_DIALECT = "POSTGRESQL";

interface Draft {
    problemId: string;
    code: string;
    savedAt: Date | null;
}

interface PendingSave {
    key: string;
    code: string;
    timer: ReturnType<typeof setTimeout>;
}

function draftKeyOf(problemId: string): string {
    return `draft:${problemId}:${DRAFT_DIALECT}`;
}

function readDraft(key: string): string | null {
    try {
        return localStorage.getItem(key);
    } catch {
        return null;
    }
}

function writeDraft(key: string, code: string): boolean {
    try {
        localStorage.setItem(key, code);
        return true;
    } catch {
        return false;
    }
}

function removeDraft(key: string): void {
    try {
        localStorage.removeItem(key);
    } catch {
        return;
    }
}

function openDraft(problemId: string, starterCode: string): Draft {
    return { problemId, code: readDraft(draftKeyOf(problemId)) ?? starterCode, savedAt: null };
}

export function useEditorDraft(problemId: string, starterCode: string) {
    const [draft, setDraft] = useState(() => openDraft(problemId, starterCode));
    const pendingSave = useRef<PendingSave | null>(null);

    if (draft.problemId !== problemId) {
        setDraft(openDraft(problemId, starterCode));
    }

    useEffect(
        () => () => {
            const pending = pendingSave.current;
            if (pending) {
                clearTimeout(pending.timer);
                writeDraft(pending.key, pending.code);
                pendingSave.current = null;
            }
        },
        [problemId],
    );

    function changeCode(code: string) {
        setDraft((current) => ({ ...current, code }));
        const key = draftKeyOf(problemId);
        clearTimeout(pendingSave.current?.timer);
        const timer = setTimeout(() => {
            pendingSave.current = null;
            if (writeDraft(key, code)) {
                setDraft((current) => ({ ...current, savedAt: new Date() }));
            }
        }, DRAFT_SAVE_DELAY_MS);
        pendingSave.current = { key, code, timer };
    }

    function resetToStarter() {
        clearTimeout(pendingSave.current?.timer);
        pendingSave.current = null;
        removeDraft(draftKeyOf(problemId));
        setDraft({ problemId, code: starterCode, savedAt: null });
    }

    return { code: draft.code, savedAt: draft.savedAt, changeCode, resetToStarter };
}
