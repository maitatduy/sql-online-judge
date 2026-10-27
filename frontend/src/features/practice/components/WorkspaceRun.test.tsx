import { fireEvent, screen, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { server } from "@/mocks/server";
import { DEMO_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { Workspace } from "./Workspace";

vi.mock("next/navigation", () => ({ notFound: vi.fn() }));

vi.mock("@/components/shared/LazySqlEditor", () => ({
    LazySqlEditor: ({
        value,
        onChange,
        ariaLabel,
    }: {
        value: string;
        onChange: (value: string) => void;
        ariaLabel: string;
    }) => (
        <textarea
            aria-label={ariaLabel}
            value={value}
            onChange={(event) => onChange(event.target.value)}
        />
    ),
}));

const SLUG = "highest-salary-per-department";

async function runCode(code?: string) {
    renderWithProviders(<Workspace slug={SLUG} />);
    const editor = await screen.findByRole("textbox", { name: "Mã SQL của bạn" });
    if (code !== undefined) {
        fireEvent.change(editor, { target: { value: code } });
    }
    fireEvent.click(screen.getByRole("button", { name: "Run" }));
    return within(screen.getByRole("tabpanel", { name: "Chạy thử" }));
}

describe("chạy thử trong Workspace", () => {
    beforeEach(() => {
        localStorage.clear();
        writeMockSession(DEMO_SESSION_USER);
    });

    it("hướng dẫn cách chạy thử khi chưa chạy lần nào", async () => {
        renderWithProviders(<Workspace slug={SLUG} />);

        const panel = within(await screen.findByRole("tabpanel", { name: "Chạy thử" }));
        expect(panel.getByText(/Bấm Run hoặc Ctrl\+Enter/)).toBeInTheDocument();
    });

    it("khoá nút Run khi đang chạy rồi hiện bảng kết quả trên dữ liệu mẫu", async () => {
        const panel = await runCode();

        expect(await panel.findByRole("status", { name: "Đang chạy thử" })).toBeInTheDocument();
        expect(screen.getByRole("button", { name: "Run" })).toBeDisabled();

        const result = within(await panel.findByRole("table", { name: "Kết quả chạy thử" }));
        expect(result.getAllByRole("row")).toHaveLength(5);
        expect(panel.getByText("4 dòng · 41 ms")).toBeInTheDocument();
        expect(screen.getByRole("button", { name: "Run" })).toBeEnabled();
    });

    it("hiện RE và thông báo lỗi cú pháp", async () => {
        const panel = await runCode("SELECT * FORM employees;");

        expect(await panel.findByText(/syntax error at or near "FORM"/)).toBeInTheDocument();
        expect(panel.getByText("RE")).toHaveAttribute("title", "Lỗi cú pháp hoặc thực thi");
    });

    it("hiện FS khi dùng lệnh bị cấm", async () => {
        const panel = await runCode("DROP TABLE employees;");

        expect(await panel.findByText("Bài này chỉ cho phép SELECT, WITH.")).toBeInTheDocument();
        expect(panel.getByText("FS")).toBeInTheDocument();
    });

    it("hiện TLE kèm giới hạn thời gian của bài", async () => {
        const panel = await runCode("SELECT pg_sleep(60);");

        expect(await panel.findByText("Quá giới hạn 2 giây.")).toBeInTheDocument();
        expect(panel.getByText("TLE")).toBeInTheDocument();
    });

    it("mời đăng nhập khi chưa đăng nhập", async () => {
        sessionStorage.clear();

        const panel = await runCode();

        expect(await panel.findByText("Đăng nhập để chạy thử.")).toBeInTheDocument();
        expect(panel.getByRole("link", { name: "Đăng nhập" })).toHaveAttribute(
            "href",
            "/login?next=%2Fproblems%2Fhighest-salary-per-department",
        );
    });

    it("báo thời gian chờ khi chạy thử quá nhanh", async () => {
        server.use(
            http.post("*/api/problems/:problemId/runs", () =>
                HttpResponse.json(
                    { status: 429, code: "RUN_RATE_LIMITED", message: "Chạy thử quá nhanh" },
                    { status: 429, headers: { "Retry-After": "12" } },
                ),
            ),
        );

        const panel = await runCode();

        expect(
            await panel.findByText("Bạn chạy thử quá nhanh. Thử lại sau 12 giây."),
        ).toBeInTheDocument();
    });
});
