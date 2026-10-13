import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { ADMIN_SESSION_USER, DEMO_SESSION_USER, writeMockSession } from "@/mocks/session";

import { useJudgeWorkers } from "./useJudgeWorkers";

function renderJudgeWorkers() {
    const queryClient = new QueryClient();
    return renderHook(() => useJudgeWorkers(), {
        wrapper: ({ children }: { children: React.ReactNode }) => (
            <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
        ),
    });
}

describe("useJudgeWorkers", () => {
    it("trả về danh sách worker kèm trạng thái", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        const { result } = renderJudgeWorkers();

        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        expect(result.current.data).toHaveLength(6);
        expect(result.current.data?.[0]).toEqual({
            name: "judge-pg-01",
            dialect: "POSTGRESQL",
            status: "BUSY",
            judgedPerHour: 212,
            cpuPercent: 92,
            disconnectedAt: null,
        });
        expect(result.current.data?.[4]).toMatchObject({
            name: "judge-my-02",
            dialect: "MYSQL",
            status: "DISCONNECTED",
            judgedPerHour: 0,
            cpuPercent: null,
        });
        expect(result.current.data?.[4].disconnectedAt).toEqual(expect.any(String));
    });

    it("báo 403 ngay, không thử lại, khi không phải quản trị viên", async () => {
        writeMockSession(DEMO_SESSION_USER);

        const { result } = renderJudgeWorkers();

        await waitFor(() => expect(result.current.isError).toBe(true));

        expect(result.current.error).toMatchObject({ status: 403, code: "ACCESS_DENIED" });
        expect(result.current.failureCount).toBe(1);
    });
});
