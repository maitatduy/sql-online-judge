import { delay, http, HttpResponse, type RequestHandler } from "msw";

import { avatarFileError } from "@/features/admin/users/avatar-file";
import type { UserAvatarResponse } from "@/features/admin/users/types";

import { userNotFound, validationFailed } from "./admin-user-errors";
import { changeAdminUser, findAdminUser } from "./admin-user-store";

const NO_CONTENT_STATUS = 204;
const BYTES_PER_CHUNK = 0x8000;

async function dataUrlOf(file: File): Promise<string> {
    const bytes = new Uint8Array(await file.arrayBuffer());
    let binary = "";
    for (let offset = 0; offset < bytes.length; offset += BYTES_PER_CHUNK) {
        binary += String.fromCharCode(...bytes.subarray(offset, offset + BYTES_PER_CHUNK));
    }
    return `data:${file.type};base64,${btoa(binary)}`;
}

export const adminUserAvatarHandlers: RequestHandler[] = [
    http.put("*/api/admin/users/:username/avatar", async ({ params, request }) => {
        await delay();
        const username = String(params.username);
        const file = (await request.formData()).get("file");
        if (file === null || typeof file === "string") {
            return validationFailed({ field: "file", message: "Chưa chọn ảnh" });
        }
        const invalidFile = avatarFileError(file);
        if (invalidFile) {
            return validationFailed({ field: "file", message: invalidFile });
        }
        if (!findAdminUser(username)) {
            return userNotFound();
        }
        const avatarUrl = await dataUrlOf(file);
        changeAdminUser(username, { avatarUrl });
        const response: UserAvatarResponse = { avatarUrl };
        return HttpResponse.json(response);
    }),

    http.delete("*/api/admin/users/:username/avatar", async ({ params }) => {
        await delay();
        const username = String(params.username);
        if (!findAdminUser(username)) {
            return userNotFound();
        }
        changeAdminUser(username, { avatarUrl: null });
        return new HttpResponse(null, { status: NO_CONTENT_STATUS });
    }),
];
