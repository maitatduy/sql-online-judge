import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type { AppNotification } from "@/lib/notifications";

import { apiError } from "./api-error";
import { readMockSession } from "./session";

let notifications: AppNotification[] = [];

function minutesAgo(minutes: number): string {
    return new Date(Date.now() - minutes * 60_000).toISOString();
}

export function resetNotifications() {
    notifications = [
        {
            id: "0192f3a0-0000-7000-f000-000000000005",
            type: "COMMENT_REPLY",
            title: "Có bình luận mới trong bài viết của bạn",
            link: "/community/0192f3a0-0000-7000-9000-000000000002",
            isRead: false,
            createdAt: minutesAgo(5),
        },
        {
            id: "0192f3a0-0000-7000-f000-000000000004",
            type: "RATING_CHANGED",
            title: "Rating của bạn tăng 24, hiện là 1.536",
            link: "/contests",
            isRead: false,
            createdAt: minutesAgo(90),
        },
        {
            id: "0192f3a0-0000-7000-f000-000000000003",
            type: "PROBLEM_PUBLISHED",
            title: 'Bài "Đếm đơn hàng theo trạng thái" đã được duyệt và xuất bản',
            link: "/problems/count-orders-by-status",
            isRead: true,
            createdAt: minutesAgo(60 * 26),
        },
    ];
}

resetNotifications();

function unauthenticated() {
    return apiError(401, "UNAUTHORIZED", "Bạn cần đăng nhập");
}

export const notificationHandlers: RequestHandler[] = [
    http.get("*/api/notifications", async ({ request }) => {
        await delay(100);
        if (!readMockSession()) {
            return unauthenticated();
        }
        const url = new URL(request.url);
        const size = Number(url.searchParams.get("size") ?? 20);
        const cursor = url.searchParams.get("cursor");
        const start = cursor ? notifications.findIndex((item) => item.id === cursor) + 1 : 0;
        const items = notifications.slice(start, start + size);
        const hasMore = start + size < notifications.length;
        return HttpResponse.json({
            items,
            nextCursor: hasMore ? items[items.length - 1].id : null,
        });
    }),
    http.get("*/api/notifications/unread-count", () =>
        readMockSession()
            ? HttpResponse.json({ count: notifications.filter((item) => !item.isRead).length })
            : unauthenticated(),
    ),
    http.post("*/api/notifications/read-all", () => {
        notifications = notifications.map((item) => ({ ...item, isRead: true }));
        return new HttpResponse(null, { status: 204 });
    }),
    http.post("*/api/notifications/:notificationId/read", ({ params }) => {
        notifications = notifications.map((item) =>
            item.id === params.notificationId ? { ...item, isRead: true } : item,
        );
        return new HttpResponse(null, { status: 204 });
    }),
];
