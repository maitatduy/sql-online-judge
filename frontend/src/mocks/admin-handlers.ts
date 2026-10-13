import { delay, http, HttpResponse, type RequestHandler } from "msw";

import { ADMIN_NAVIGATION_COUNTS, buildAdminOverview } from "./admin-fixtures";
import { apiError } from "./api-error";
import { readMockSession } from "./session";

export const adminHandlers: RequestHandler[] = [
    http.all("*/api/admin/*", () => {
        const currentUser = readMockSession();
        if (!currentUser) {
            return apiError(401, "UNAUTHENTICATED", "Bạn chưa đăng nhập");
        }
        if (currentUser.role !== "ADMIN") {
            return apiError(403, "ACCESS_DENIED", "Bạn không có quyền thực hiện thao tác này");
        }
        return undefined;
    }),

    http.get("*/api/admin/navigation-counts", async () => {
        await delay();
        return HttpResponse.json(ADMIN_NAVIGATION_COUNTS);
    }),

    http.get("*/api/admin/overview", async () => {
        await delay();
        return HttpResponse.json(buildAdminOverview());
    }),
];
