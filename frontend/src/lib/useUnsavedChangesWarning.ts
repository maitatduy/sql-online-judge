import { useEffect } from "react";

export function useUnsavedChangesWarning(hasUnsavedChanges: boolean): void {
    useEffect(() => {
        if (!hasUnsavedChanges) {
            return undefined;
        }
        const warn = (event: BeforeUnloadEvent) => {
            event.preventDefault();
        };
        window.addEventListener("beforeunload", warn);
        return () => window.removeEventListener("beforeunload", warn);
    }, [hasUnsavedChanges]);
}
