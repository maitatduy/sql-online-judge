import { adminApi } from "../api";
import { useAdminUserMutation } from "./useAdminUserMutation";

export function useUnbanUser() {
    return useAdminUserMutation(adminApi.unbanUser);
}
