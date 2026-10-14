import { adminApi } from "../api";
import { useAdminUserMutation } from "./useAdminUserMutation";

export function useRestoreUser() {
    return useAdminUserMutation(adminApi.restoreUser);
}
