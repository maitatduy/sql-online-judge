import { adminApi } from "../api";
import { useAdminUserMutation } from "./useAdminUserMutation";

export function useChangeUserRole() {
    return useAdminUserMutation(adminApi.changeUserRole);
}
