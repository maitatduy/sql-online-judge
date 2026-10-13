import { useQueryClient } from "@tanstack/react-query";
import { useEffect, useRef } from "react";

import { useToast } from "@/components/shared/ToastProvider";

import type { JudgeWorker } from "../types";
import { findRecoveredWorkers } from "../worker-status";

export function useWorkerRecoveryToast(workers: JudgeWorker[] | undefined): void {
    const queryClient = useQueryClient();
    const showToast = useToast();
    const previousWorkers = useRef<JudgeWorker[]>([]);

    useEffect(() => {
        if (!workers) {
            return;
        }
        const recoveredNames = findRecoveredWorkers(previousWorkers.current, workers);
        previousWorkers.current = workers;
        if (recoveredNames.length === 0) {
            return;
        }
        recoveredNames.forEach((name) => showToast(`${name} đã hoạt động trở lại.`));
        void queryClient.invalidateQueries({ queryKey: ["admin", "overview"] });
        void queryClient.invalidateQueries({ queryKey: ["admin", "navigation-counts"] });
    }, [workers, queryClient, showToast]);
}
