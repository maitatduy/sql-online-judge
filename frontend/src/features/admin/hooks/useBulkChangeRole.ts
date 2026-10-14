import { adminApi } from "../api";
import { useBulkUserMutation } from "./useBulkUserMutation";

export function useBulkChangeRole() {
    return useBulkUserMutation(adminApi.bulkChangeRole);
}
