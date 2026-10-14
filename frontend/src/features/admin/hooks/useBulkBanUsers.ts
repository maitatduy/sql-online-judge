import { adminApi } from "../api";
import { useBulkUserMutation } from "./useBulkUserMutation";

export function useBulkBanUsers() {
    return useBulkUserMutation(adminApi.bulkBanUsers);
}
