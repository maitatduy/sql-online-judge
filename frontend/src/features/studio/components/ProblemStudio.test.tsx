import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { server } from "@/mocks/server";
import { ADMIN_SESSION_USER, SETTER_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { ProblemStudio } from "./ProblemStudio";

const DRAFT_ID = "0192f3d0-0000-7000-8000-000000000001";
const PENDING_ID = "0192f3d0-0000-7000-8000-000000000004";

function renderStudio(problemId = DRAFT_ID) {
    renderWithProviders(
        <ToastProvider>
            <ProblemStudio problemId={problemId} />
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

describe("ProblemStudio - bước Thông tin", () => {
    beforeEach(() => {
        writeMockSession(SETTER_SESSION_USER);
        server.events.removeAllListeners();
    });

    it("mở bài nháp với thông tin hiện tại và khung 5 bước", async () => {
        renderStudio();

        expect(await screen.findByRole("textbox", { name: "Tên bài" })).toHaveValue(
            "Phễu chuyển đổi 4 bước",
        );
        expect(screen.getByRole("textbox", { name: "Slug" })).toHaveValue(
            "phieu-chuyen-doi-4-buoc",
        );
        const steps = within(screen.getByRole("navigation", { name: "Các bước soạn bài" }));
        expect(steps.getByRole("link", { name: /Thông tin/ })).toHaveAttribute(
            "aria-current",
            "step",
        );
        expect(steps.getByText("Dataset và schema")).toBeInTheDocument();
        expect(screen.getByRole("link", { name: "Quay lại Bài của tôi" })).toHaveAttribute(
            "href",
            "/studio/problems",
        );
    });

    it("tự lưu ô vừa sửa, chỉ gửi đúng ô đó, rồi báo đã lưu", async () => {
        const patches = recordPatches();
        renderStudio();

        fireEvent.change(await screen.findByRole("textbox", { name: "Tên bài" }), {
            target: { value: "Phễu chuyển đổi 5 bước" },
        });

        expect(await screen.findByText(/^Bản nháp · lưu \d{2}:\d{2}$/)).toBeInTheDocument();
        expect(patches).toEqual([{ title: "Phễu chuyển đổi 5 bước" }]);
    });

    it("gộp các thay đổi liên tiếp vào một lần lưu", async () => {
        const patches = recordPatches();
        renderStudio();

        fireEvent.change(await screen.findByRole("combobox", { name: "Độ khó" }), {
            target: { value: "HARD" },
        });
        fireEvent.click(await screen.findByRole("checkbox", { name: "JOIN" }));
        fireEvent.click(screen.getByRole("checkbox", { name: "CTE" }));

        await screen.findByText(/^Bản nháp · lưu/);
        expect(patches).toEqual([{ difficulty: "HARD", tagSlugs: ["join", "cte"] }]);
    });

    it("báo lỗi slug sai định dạng ngay, không gửi lên máy chủ", async () => {
        const patches = recordPatches();
        renderStudio();

        fireEvent.change(await screen.findByRole("textbox", { name: "Slug" }), {
            target: { value: "Phễu chuyển đổi" },
        });

        expect(
            await screen.findByText("Chỉ gồm chữ thường không dấu, số và dấu gạch nối"),
        ).toBeInTheDocument();
        await new Promise((resolve) => setTimeout(resolve, 1200));
        expect(patches).toEqual([]);
    });

    it("hiện lỗi dưới ô slug khi slug đã có bài khác dùng", async () => {
        renderStudio();

        fireEvent.change(await screen.findByRole("textbox", { name: "Slug" }), {
            target: { value: "classmate-pairs" },
        });

        expect(await screen.findByText("Slug đã được bài khác dùng")).toBeInTheDocument();
        expect(screen.getByRole("textbox", { name: "Slug" })).toHaveAttribute(
            "aria-invalid",
            "true",
        );
    });

    it("khoá các ô khi bài đang chờ duyệt", async () => {
        renderStudio(PENDING_ID);

        expect(
            await screen.findByText("Bài đang chờ duyệt nên chưa sửa được."),
        ).toBeInTheDocument();
        expect(screen.getByRole("textbox", { name: "Tên bài" })).toBeDisabled();
    });

    it("cho admin mở bài của setter khác", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        renderStudio();

        expect(await screen.findByRole("textbox", { name: "Tên bài" })).toHaveValue(
            "Phễu chuyển đổi 4 bước",
        );
    });

    it("báo không tìm thấy khi bài không tồn tại hoặc không phải của mình", async () => {
        renderStudio("0192f3d0-0000-7000-8000-ffffffffffff");

        await waitFor(() =>
            expect(screen.getByText("Không tìm thấy bài này.")).toBeInTheDocument(),
        );
    });
});
