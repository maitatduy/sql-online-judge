import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { ADMIN_SESSION_USER, DEMO_SESSION_USER, writeMockSession } from "@/mocks/session";

import { usePendingTasks } from "./usePendingTasks";

function renderPendingTasks() {
    const queryClient = new QueryClient();
    return renderHook(() => usePendingTasks(), {
        wrapper: ({ children }: { children: React.ReactNode }) => (
            <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
        ),
    });
}

describe("usePendingTasks", () => {
    it("trả về các nhóm việc đang chờ quản trị viên", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        const { result } = renderPendingTasks();

        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        expect(result.current.data).toEqual([
            { type: "PROBLEM_REVIEW", count: 5, detail: "Cũ nhất gửi 3 ngày trước" },
            {
                type: "ABUSE_REPORT",
                count: 12,
                detail: "4 báo cáo về cùng một bình luận ở Weekly #37",
            },
            { type: "AI_FLAG", count: 3, detail: "Cố lấy đáp án đầy đủ trước khi AC" },
            { type: "SUPPORT_MESSAGE", count: 3, detail: "Mới nhất lúc 15:20 từ hoa.kieu" },
        ]);
    });

    it("báo 403 ngay, không thử lại, khi không phải quản trị viên", async () => {
        writeMockSession(DEMO_SESSION_USER);

        const { result } = renderPendingTasks();

        await waitFor(() => expect(result.current.isError).toBe(true));

        expect(result.current.error).toMatchObject({ status: 403, code: "ACCESS_DENIED" });
        expect(result.current.failureCount).toBe(1);
    });
});
