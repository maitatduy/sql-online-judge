import { useQuery } from "@tanstack/react-query";

import { adminApi } from "../api";
import { workersRefreshMs } from "../worker-status";

export function useJudgeWorkers() {
    return useQuery({
        queryKey: ["admin", "judge-workers"],
        queryFn: ({ signal }) => adminApi.getJudgeWorkers(signal),
        select: (response) => response.items,
        refetchInterval: (query) =>
            workersRefreshMs(query.state.data?.items.map((worker) => worker.status) ?? []),
        retry: false,
    });
}
