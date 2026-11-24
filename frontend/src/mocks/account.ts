import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type {
    AccountProfile,
    ChangePasswordRequest,
    UpdateAccountProfileRequest,
} from "@/features/settings/types";

import { apiError } from "./api-error";
import { MINH_ANH_PROFILE } from "./profile-fixtures";
import { readMockSession } from "./session";

export const MOCK_CURRENT_PASSWORD = "matkhau-hien-tai";
const MOCK_AVATAR_URL = "https://res.cloudinary.com/demo/image/upload/avatar-demo.png";

function initialAccount(): AccountProfile {
    const { profile } = MINH_ANH_PROFILE;
    return {
        username: profile.username,
        email: "minh.anh@example.com",
        displayName: profile.displayName ?? profile.username,
        bio: profile.bio,
        school: profile.school,
        location: profile.location,
        websiteUrl: profile.websiteUrl,
        avatarUrl: null,
        hasPassword: true,
    };
}

let account = initialAccount();

export function resetMockAccount(): void {
    account = initialAccount();
}

function blankToNull(value: string): string | null {
    return value.trim() === "" ? null : value.trim();
}

function unauthenticated() {
    return apiError(401, "UNAUTHORIZED", "Bạn cần đăng nhập");
}

export const accountHandlers: RequestHandler[] = [
    http.get("*/api/account/profile", async () => {
        await delay();
        return readMockSession() ? HttpResponse.json(account) : unauthenticated();
    }),

    http.patch("*/api/account/profile", async ({ request }) => {
        await delay();
        if (!readMockSession()) {
            return unauthenticated();
        }
        const body = (await request.json()) as UpdateAccountProfileRequest;
        account = {
            ...account,
            displayName: body.displayName.trim(),
            bio: blankToNull(body.bio),
            school: blankToNull(body.school),
            location: blankToNull(body.location),
            websiteUrl: blankToNull(body.websiteUrl),
        };
        return HttpResponse.json(account);
    }),

    http.post("*/api/account/password", async ({ request }) => {
        await delay();
        if (!readMockSession()) {
            return unauthenticated();
        }
        const body = (await request.json()) as ChangePasswordRequest;
        if (account.hasPassword && body.currentPassword !== MOCK_CURRENT_PASSWORD) {
            return apiError(400, "VALIDATION_FAILED", "Dữ liệu gửi lên không hợp lệ", {
                errors: [{ field: "currentPassword", message: "Mật khẩu hiện tại không đúng" }],
            });
        }
        account = { ...account, hasPassword: true };
        return new HttpResponse(null, { status: 204 });
    }),

    http.put("*/api/account/avatar", async () => {
        await delay();
        if (!readMockSession()) {
            return unauthenticated();
        }
        account = { ...account, avatarUrl: MOCK_AVATAR_URL };
        return HttpResponse.json({ avatarUrl: MOCK_AVATAR_URL });
    }),

    http.delete("*/api/account/avatar", async () => {
        await delay();
        if (!readMockSession()) {
            return unauthenticated();
        }
        account = { ...account, avatarUrl: null };
        return new HttpResponse(null, { status: 204 });
    }),
];
