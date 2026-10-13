import { delay, http, HttpResponse, type RequestHandler } from "msw";

import {
    ADMIN_PENDING_TASKS,
    buildAdminOverview,
    buildJudgeWorkers,
    buildNavigationCounts,
} from "./admin-fixtures";
import { apiError } from "./api-error";
import { readMockSession } from "./session";
import { readWorkerRestarts, recordWorkerRestart } from "./worker-restarts";

const MAX_RESTART_REASON_LENGTH = 500;

function reasonError(reason: unknown): string | null {
    if (typeof reason !== "string" || reason.trim() === "") {
        return "Không được để trống";
    }
    return reason.length > MAX_RESTART_REASON_LENGTH ? "Tối đa 500 ký tự" : null;
}

function currentWorkers() {
    return buildJudgeWorkers(new Date(), readWorkerRestarts());
}

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
        return HttpResponse.json(buildNavigationCounts(currentWorkers()));
    }),

    http.get("*/api/admin/overview", async () => {
        await delay();
        return HttpResponse.json(buildAdminOverview(currentWorkers()));
    }),

    http.get("*/api/admin/judge/workers", async () => {
        await delay();
        return HttpResponse.json({ items: currentWorkers() });
    }),

    http.post("*/api/admin/judge/workers/:name/restart", async ({ params, request }) => {
        await delay();
        const name = String(params.name);
        const body = (await request.json()) as { reason?: unknown };
        const invalidReason = reasonError(body.reason);
        if (invalidReason) {
            return apiError(400, "VALIDATION_FAILED", "Dữ liệu gửi lên không hợp lệ", {
                errors: [{ field: "reason", message: invalidReason }],
            });
        }
        const now = new Date();
        const worker = buildJudgeWorkers(now, readWorkerRestarts()).find(
            (candidate) => candidate.name === name,
        );
        if (!worker) {
            return apiError(404, "WORKER_NOT_FOUND", "Không tìm thấy worker");
        }
        if (worker.status === "RESTARTING") {
            return apiError(409, "WORKER_ALREADY_RESTARTING", "Worker đang khởi động lại");
        }
        recordWorkerRestart(name, now.getTime());
        return HttpResponse.json({ name, status: "RESTARTING" }, { status: 202 });
    }),

    http.get("*/api/admin/pending-tasks", async () => {
        await delay();
        return HttpResponse.json({ items: ADMIN_PENDING_TASKS });
    }),
];
