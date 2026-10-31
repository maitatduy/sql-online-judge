import { fireEvent, screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { server } from "@/mocks/server";
import { SETTER_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { ProblemStudio } from "./ProblemStudio";

const DRAFT_ID = "0192f3d0-0000-7000-8000-000000000001";
const PENDING_ID = "0192f3d0-0000-7000-8000-000000000004";
const MAX_STATEMENT_LENGTH = 51_200;

function renderStatement(problemId = DRAFT_ID) {
    renderWithProviders(
        <ToastProvider>
            <ProblemStudio problemId={problemId} step="statement" />
        </ToastProvider>,
    );
}

function recordPatches(): unknown[] {
    const bodies: unknown[] = [];
    server.events.on("request:start", async ({ request }) => {
        if (request.method === "PATCH") {
            bodies.push(await request.clone().json());
        }
    });
    return bodies;
}

describe("ProblemStudio - bước Đề bài", () => {
    beforeEach(() => {
        writeMockSession(SETTER_SESSION_USER);
        server.events.removeAllListeners();
    });

    it("đánh dấu bước Đề bài và cho quay lại bước Thông tin", async () => {
        renderStatement();

        await screen.findByRole("textbox", { name: "Đề bài" });
        const steps = within(screen.getByRole("navigation", { name: "Các bước soạn bài" }));
        expect(steps.getByRole("link", { name: /Đề bài/ })).toHaveAttribute("aria-current", "step");
        expect(steps.getByRole("link", { name: /Thông tin/ })).toHaveAttribute(
            "href",
            `/studio/problems/${DRAFT_ID}`,
        );
        expect(steps.getByRole("link", { name: /Thông tin/ })).not.toHaveAttribute("aria-current");
    });

    it("tự lưu đề bài vừa soạn", async () => {
        const patches = recordPatches();
        renderStatement();

        fireEvent.change(await screen.findByRole("textbox", { name: "Đề bài" }), {
            target: { value: "## Yêu cầu\nTính tỉ lệ chuyển đổi." },
        });

        expect(await screen.findByText(/^Bản nháp · lưu \d{2}:\d{2}$/)).toBeInTheDocument();
        expect(patches).toEqual([{ statementMarkdown: "## Yêu cầu\nTính tỉ lệ chuyển đổi." }]);
    });

    it("xem trước đề bài dưới dạng Markdown", async () => {
        renderStatement();

        fireEvent.change(await screen.findByRole("textbox", { name: "Đề bài" }), {
            target: { value: "## Yêu cầu" },
        });
        fireEvent.mouseDown(screen.getByRole("tab", { name: "Xem trước" }));

        expect(await screen.findByRole("heading", { name: "Yêu cầu" })).toBeInTheDocument();
    });

    it("báo lỗi khi đề bài quá dài, không gửi lên máy chủ", async () => {
        const patches = recordPatches();
        renderStatement();

        fireEvent.change(await screen.findByRole("textbox", { name: "Đề bài" }), {
            target: { value: "a".repeat(MAX_STATEMENT_LENGTH + 1) },
        });

        expect(await screen.findByText("Đề bài tối đa 50 KB")).toBeInTheDocument();
        await new Promise((resolve) => setTimeout(resolve, 1200));
        expect(patches).toEqual([]);
    });

    it("khoá ô soạn khi bài đang chờ duyệt", async () => {
        renderStatement(PENDING_ID);

        expect(await screen.findByRole("textbox", { name: "Đề bài" })).toBeDisabled();
        expect(screen.getByText("Bài đang chờ duyệt nên chưa sửa được.")).toBeInTheDocument();
    });
});
