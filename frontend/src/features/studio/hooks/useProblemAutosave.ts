import { useEffect, useRef, useState } from "react";

import { useMutation, useQueryClient } from "@tanstack/react-query";

import { studioApi } from "../api";
import type { StudioProblem, UpdateProblemRequest } from "../types";
import { studioProblemQueryKey } from "./useStudioProblem";

export const AUTOSAVE_DELAY_MS = 800;

export function useProblemAutosave(problemId: string, onError: (error: Error) => void) {
    const queryClient = useQueryClient();
    const [savedAt, setSavedAt] = useState<string | null>(null);
    const pending = useRef<UpdateProblemRequest>({});
    const timer = useRef<ReturnType<typeof setTimeout>>(undefined);
    const save = useMutation({
        mutationFn: (request: UpdateProblemRequest) => studioApi.updateProblem(problemId, request),
        onSuccess: (problem: StudioProblem) => {
            setSavedAt(problem.updatedAt);
            queryClient.setQueryData(studioProblemQueryKey(problemId), problem);
            void queryClient.invalidateQueries({ queryKey: ["studio-problems"] });
        },
        onError,
    });
    const saveRef = useRef(save.mutate);

    useEffect(() => {
        saveRef.current = save.mutate;
    }, [save.mutate]);

    useEffect(
        () => () => {
            clearTimeout(timer.current);
            if (Object.keys(pending.current).length > 0) {
                saveRef.current(pending.current);
            }
        },
        [],
    );

    function queueSave(change: UpdateProblemRequest) {
        pending.current = { ...pending.current, ...change };
        clearTimeout(timer.current);
        timer.current = setTimeout(() => {
            const request = pending.current;
            pending.current = {};
            saveRef.current(request);
        }, AUTOSAVE_DELAY_MS);
    }

    return { queueSave, isSaving: save.isPending, error: save.error, savedAt };
}
