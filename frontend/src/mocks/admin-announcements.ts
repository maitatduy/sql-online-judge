import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type { Announcement } from "@/features/admin/announcements/api";

import { apiError } from "./api-error";
import { readMockSession } from "./session";

const MOCK_RECIPIENTS = 2_431;

let announcements: Announcement[] = [];

export function resetAnnouncements() {
    announcements = [
        {
            id: "0192f3a0-0000-7000-f100-000000000001",
            title: "Contest SQL Cup tháng 12 mở đăng ký",
            link: "/contests",
            recipientCount: MOCK_RECIPIENTS,
            isDelivered: true,
            createdAt: new Date(Date.now() - 2 * 86_400_000).toISOString(),
        },
    ];
}

resetAnnouncements();

function forbidden() {
    return apiError(403, "ACCESS_DENIED", "Bạn không có quyền");
}

export const adminAnnouncementHandlers: RequestHandler[] = [
    http.get("*/api/admin/announcements", async () => {
        await delay(100);
        if (readMockSession()?.role !== "ADMIN") {
            return forbidden();
        }
        return HttpResponse.json({
            items: announcements,
            page: 1,
            size: 20,
            totalItems: announcements.length,
            totalPages: 1,
        });
    }),
    http.post("*/api/admin/announcements", async ({ request }) => {
        if (readMockSession()?.role !== "ADMIN") {
            return forbidden();
        }
        const { title, link } = (await request.json()) as { title: string; link?: string };
        if (link && !link.startsWith("/")) {
            return apiError(400, "VALIDATION_FAILED", "Dữ liệu gửi lên không hợp lệ", {
                errors: [{ field: "link", message: "Đường dẫn phải bắt đầu bằng /" }],
            });
        }
        const announcement: Announcement = {
            id: crypto.randomUUID(),
            title,
            link: link ?? null,
            recipientCount: MOCK_RECIPIENTS,
            isDelivered: true,
            createdAt: new Date().toISOString(),
        };
        announcements = [announcement, ...announcements];
        return HttpResponse.json(announcement, { status: 202 });
    }),
];
