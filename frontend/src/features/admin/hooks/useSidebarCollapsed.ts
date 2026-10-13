import { useState } from "react";

const STORAGE_KEY = "sqljudge:admin:sidebar-collapsed";

function readStoredChoice(): boolean {
    try {
        return localStorage.getItem(STORAGE_KEY) === "true";
    } catch {
        return false;
    }
}

function storeChoice(isCollapsed: boolean): boolean {
    try {
        localStorage.setItem(STORAGE_KEY, String(isCollapsed));
        return true;
    } catch {
        return false;
    }
}

export function useSidebarCollapsed() {
    const [isCollapsed, setIsCollapsed] = useState(readStoredChoice);

    function toggleCollapsed() {
        const nextIsCollapsed = !isCollapsed;
        setIsCollapsed(nextIsCollapsed);
        storeChoice(nextIsCollapsed);
    }

    return { isCollapsed, toggleCollapsed };
}
