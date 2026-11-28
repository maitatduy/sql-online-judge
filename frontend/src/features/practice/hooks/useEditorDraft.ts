import { useEffect, useRef, useState } from "react";

import type { Dialect } from "@/lib/dialect";
import { draftKeyOf } from "@/lib/workspace-storage";

export const DRAFT_SAVE_DELAY_MS = 500;

interface Draft {
    key: string;
    code: string;
}

interface PendingSave {
    key: string;
    code: string;
    timer: ReturnType<typeof setTimeout>;
}

function readDraft(key: string): string | null {
    try {
        return localStorage.getItem(key);
    } catch {
        return null;
    }
}

function writeDraft(key: string, code: string): void {
    try {
        localStorage.setItem(key, code);
    } catch {
        return;
    }
}

function removeDraft(key: string): void {
    try {
        localStorage.removeItem(key);
    } catch {
        return;
    }
}

function openDraft(key: string, starterCode: string): Draft {
    return { key, code: readDraft(key) ?? starterCode };
}

export function useEditorDraft(problemId: string, dialect: Dialect, starterCode: string) {
    const key = draftKeyOf(problemId, dialect);
    const [draft, setDraft] = useState(() => openDraft(key, starterCode));
    const pendingSave = useRef<PendingSave | null>(null);

    if (draft.key !== key) {
        setDraft(openDraft(key, starterCode));
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
        [key],
    );

    function changeCode(code: string) {
        setDraft((current) => ({ ...current, code }));
        clearTimeout(pendingSave.current?.timer);
        const timer = setTimeout(() => {
            pendingSave.current = null;
            writeDraft(key, code);
        }, DRAFT_SAVE_DELAY_MS);
        pendingSave.current = { key, code, timer };
    }

    function resetToStarter() {
        clearTimeout(pendingSave.current?.timer);
        pendingSave.current = null;
        removeDraft(key);
        setDraft({ key, code: starterCode });
    }

    return { code: draft.code, changeCode, resetToStarter };
}
