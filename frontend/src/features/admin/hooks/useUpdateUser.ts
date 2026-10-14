import { adminApi } from "../api";
import { useAdminUserMutation } from "./useAdminUserMutation";

export function useUpdateUser() {
    return useAdminUserMutation(adminApi.updateUser);
}
