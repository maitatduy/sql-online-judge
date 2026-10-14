import { useMutation } from "@tanstack/react-query";

import { adminApi } from "../api";

export function useExportUsers() {
    return useMutation({ mutationFn: adminApi.exportUsers });
}
