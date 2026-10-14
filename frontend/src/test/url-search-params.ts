import { useMemo, useSyncExternalStore } from "react";

const listeners = new Set<() => void>();
const replaceHistoryState = window.history.replaceState.bind(window.history);

window.history.replaceState = (...args: Parameters<History["replaceState"]>) => {
    replaceHistoryState(...args);
    listeners.forEach((notify) => notify());
};

function subscribe(listener: () => void): () => void {
    listeners.add(listener);
    return () => listeners.delete(listener);
}

export function useUrlSearchParams(): URLSearchParams {
    const search = useSyncExternalStore(subscribe, () => window.location.search);
    return useMemo(() => new URLSearchParams(search), [search]);
}

export function visit(path: string): void {
    window.history.replaceState(null, "", path);
}
