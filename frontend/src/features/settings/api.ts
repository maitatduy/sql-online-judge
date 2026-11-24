import { apiClient } from "@/lib/api-client";

import type {
    AccountAvatar,
    AccountProfile,
    ChangePasswordRequest,
    UpdateAccountProfileRequest,
} from "./types";

const PROFILE_PATH = "/api/account/profile";
const AVATAR_PATH = "/api/account/avatar";

export function getAccountProfile(signal?: AbortSignal): Promise<AccountProfile> {
    return apiClient.get<AccountProfile>(PROFILE_PATH, { signal });
}

export function updateAccountProfile(
    request: UpdateAccountProfileRequest,
): Promise<AccountProfile> {
    return apiClient.patch<AccountProfile>(PROFILE_PATH, request);
}

export function changePassword(request: ChangePasswordRequest): Promise<void> {
    return apiClient.post<void>("/api/account/password", request);
}

export function replaceAvatar(file: File): Promise<AccountAvatar> {
    const form = new FormData();
    form.append("file", file);
    return apiClient.putForm<AccountAvatar>(AVATAR_PATH, form);
}

export function removeAvatar(): Promise<void> {
    return apiClient.delete<void>(AVATAR_PATH);
}
