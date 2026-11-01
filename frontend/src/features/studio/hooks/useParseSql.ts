import { useMutation } from "@tanstack/react-query";

import { studioApi } from "../api";

export function useParseSql() {
    return useMutation({ mutationFn: studioApi.parseSql });
}
