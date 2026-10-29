import { fireEvent, screen, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { server } from "@/mocks/server";
import { DEMO_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import type { ProblemSubmissionSummary } from "../types";
import { Workspace } from "./Workspace";

vi.mock("next/navigation", () => ({ notFound: vi.fn() }));

vi.mock("@/components/shared/LazySqlEditor", () => ({
    LazySqlEditor: ({ value, ariaLabel }: { value: string; ariaLabel: string }) => (
        <textarea aria-label={ariaLabel} value={value} readOnly />
    ),
}));

const SLUG = "highest-salary-per-department";

function historyItem(id: string, overrides: Partial<ProblemSubmissionSummary> = {}) {
    return {
        id,
        status: "COMPLETED",
        verdict: "AC",
        dialect: "POSTGRESQL",
        runtimeMs: 48,
        submittedAt: new Date().toISOString(),
        ...overrides,
    } satisfies ProblemSubmissionSummary;
}

async function openHistory(slug = SLUG) {
    renderWithProviders(<Workspace slug={slug} />);
    fireEvent.mouseDown(await screen.findByRole("tab", { name: "Lịch sử nộp" }));
    return within(await screen.findByRole("tabpanel", { name: "Lịch sử nộp" }));
}

describe("lịch sử nộp trong Workspace", () => {
    beforeEach(() => {
        localStorage.clear();
        writeMockSession(DEMO_SESSION_USER);
    });

    it("liệt kê bài nộp của bạn với verdict, dialect, thời gian chạy và lúc nộp", async () => {
        const panel = await openHistory();

        const table = within(await panel.findByRole("table", { name: "Lịch sử nộp" }));
        const [, latest, oldest] = table.getAllByRole("row");
        expect(latest).toHaveAccessibleName(/^WA PostgreSQL 41 ms 1 ngày trước$/);
        expect(oldest).toHaveAccessibleName(/^RE PostgreSQL - 1 ngày trước$/);
    });

    it("báo khi chưa nộp bài này lần nào", async () => {
        const panel = await openHistory("customers-who-never-order");

        expect(await panel.findByText("Bạn chưa nộp bài này lần nào.")).toBeInTheDocument();
    });

    it("mời đăng nhập khi chưa đăng nhập", async () => {
        sessionStorage.clear();

        const panel = await openHistory();

        expect(await panel.findByText("Đăng nhập để xem lịch sử nộp.")).toBeInTheDocument();
        expect(panel.getByRole("link", { name: "Đăng nhập" })).toHaveAttribute(
            "href",
            "/login?next=%2Fproblems%2Fhighest-salary-per-department",
        );
    });

    it("thêm bài vừa nộp vào lịch sử sau khi chấm xong", async () => {
        renderWithProviders(<Workspace slug={SLUG} />);
        await screen.findByRole("textbox", { name: "Mã SQL của bạn" });
        fireEvent.click(screen.getByRole("button", { name: "Submit" }));
        const result = within(screen.getByRole("tabpanel", { name: "Nộp bài" }));
        await result.findByText("Sai ở testcase 2/3 · 48 ms");

        fireEvent.mouseDown(screen.getByRole("tab", { name: "Lịch sử nộp" }));

        const panel = within(await screen.findByRole("tabpanel", { name: "Lịch sử nộp" }));
        const table = within(await panel.findByRole("table", { name: "Lịch sử nộp" }));
        expect(table.getAllByRole("row")).toHaveLength(4);
        expect(table.getAllByRole("row")[1]).toHaveAccessibleName(/^WA PostgreSQL 48 ms Vừa xong$/);
    });

    it("tải thêm trang tiếp theo", async () => {
        server.use(
            http.get("*/api/problems/:problemId/submissions", ({ request }) => {
                const cursor = new URL(request.url).searchParams.get("cursor");
                return HttpResponse.json(
                    cursor === "next"
                        ? { items: [historyItem("second", { verdict: "TLE" })], nextCursor: null }
                        : { items: [historyItem("first")], nextCursor: "next" },
                );
            }),
        );

        const panel = await openHistory();
        fireEvent.click(await panel.findByRole("button", { name: "Xem thêm" }));

        expect(await panel.findByText("TLE")).toBeInTheDocument();
        expect(panel.getByText("AC")).toBeInTheDocument();
        expect(panel.queryByRole("button", { name: "Xem thêm" })).not.toBeInTheDocument();
    });
});
