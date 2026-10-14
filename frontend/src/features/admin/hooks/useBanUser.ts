import { adminApi } from "../api";
import { useAdminUserMutation } from "./useAdminUserMutation";

export function useBanUser() {
    return useAdminUserMutation(adminApi.banUser);
}
