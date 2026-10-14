import { adminApi } from "../api";
import { useAdminUserMutation } from "./useAdminUserMutation";

export function useCreateUser() {
    return useAdminUserMutation(adminApi.createUser);
}
