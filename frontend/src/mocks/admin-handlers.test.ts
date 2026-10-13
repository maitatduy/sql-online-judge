import { describe, expect, it } from "vitest";

import type { AdminOverviewResponse, JudgeWorkersResponse } from "@/features/admin/types";
import type { AdminNavigationCounts } from "@/lib/admin-navigation";
import { apiClient } from "@/lib/api-client";
import type { CurrentUser } from "@/lib/current-user";

import { ADMIN_SESSION_USER, DEMO_SESSION_USER, writeMockSession } from "./session";
import { recordWorkerRestart } from "./worker-restarts";

describe("API quản trị giả", () => {
    it("đăng nhập bằng tài khoản quản trị trả về vai trò ADMIN", async () => {
        await apiClient.post("/api/auth/login", {
            email: "admin@sqljudge.dev",
            password: "mock-password",
            rememberMe: false,
        });

        const currentUser = await apiClient.get<CurrentUser>("/api/auth/me");

        expect(currentUser).toMatchObject({ username: "admin.huy", role: "ADMIN" });
    });

    it("trả 401 cho mọi đường dẫn quản trị khi chưa đăng nhập", async () => {
        await expect(apiClient.get("/api/admin/overview")).rejects.toMatchObject({
            status: 401,
            code: "UNAUTHENTICATED",
        });
    });

    it("trả 403 cho mọi đường dẫn quản trị khi không phải quản trị viên", async () => {
        writeMockSession(DEMO_SESSION_USER);

        await expect(
            apiClient.post("/api/admin/judge/workers/judge-my-02/restart", { reason: "Thử" }),
        ).rejects.toMatchObject({ status: 403, code: "ACCESS_DENIED" });
    });
});

describe("POST /api/admin/judge/workers/{name}/restart", () => {
    const restartPath = "/api/admin/judge/workers/judge-my-02/restart";

    it("nhận yêu cầu và chuyển worker sang đang khởi động lại", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(apiClient.post(restartPath, { reason: "Worker treo" })).resolves.toEqual({
            name: "judge-my-02",
            status: "RESTARTING",
        });

        const { items } = await apiClient.get<JudgeWorkersResponse>("/api/admin/judge/workers");

        expect(items.find((worker) => worker.name === "judge-my-02")).toMatchObject({
            status: "RESTARTING",
            cpuPercent: null,
            disconnectedAt: null,
        });
    });

    it("trả 400 kèm lỗi theo trường khi thiếu lý do", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(apiClient.post(restartPath, { reason: "  " })).rejects.toMatchObject({
            status: 400,
            code: "VALIDATION_FAILED",
            errors: [{ field: "reason", message: "Không được để trống" }],
        });
    });

    it("trả 400 khi lý do dài quá 500 ký tự", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(
            apiClient.post(restartPath, { reason: "a".repeat(501) }),
        ).rejects.toMatchObject({ status: 400, code: "VALIDATION_FAILED" });
    });

    it("trả 404 khi không có worker", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(
            apiClient.post("/api/admin/judge/workers/judge-xx-99/restart", { reason: "Thử" }),
        ).rejects.toMatchObject({ status: 404, code: "WORKER_NOT_FOUND" });
    });

    it("trả 409 khi worker đang khởi động lại", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        await apiClient.post(restartPath, { reason: "Worker treo" });

        await expect(apiClient.post(restartPath, { reason: "Lần nữa" })).rejects.toMatchObject({
            status: 409,
            code: "WORKER_ALREADY_RESTARTING",
        });
    });
});

describe("số liệu sau khi worker hoạt động lại", () => {
    const LONG_AGO_MS = 60_000;

    it("hàng đợi cao và sidebar báo sự cố khi còn worker mất kết nối", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        const overview = await apiClient.get<AdminOverviewResponse>("/api/admin/overview");
        const counts = await apiClient.get<AdminNavigationCounts>("/api/admin/navigation-counts");

        expect(overview.queue).toEqual({ size: 146, averageWaitMs: 3800, isDegraded: true });
        expect(counts.hasJudgeIncident).toBe(true);
    });

    it("hàng đợi về bình thường và sidebar hết báo sự cố khi mọi worker hoạt động", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        recordWorkerRestart("judge-my-02", Date.now() - LONG_AGO_MS);

        const overview = await apiClient.get<AdminOverviewResponse>("/api/admin/overview");
        const counts = await apiClient.get<AdminNavigationCounts>("/api/admin/navigation-counts");
        const { items } = await apiClient.get<JudgeWorkersResponse>("/api/admin/judge/workers");

        expect(overview.queue).toEqual({ size: 107, averageWaitMs: 2100, isDegraded: false });
        expect(counts.hasJudgeIncident).toBe(false);
        expect(items.find((worker) => worker.name === "judge-my-02")?.status).toBe("ONLINE");
    });
});
