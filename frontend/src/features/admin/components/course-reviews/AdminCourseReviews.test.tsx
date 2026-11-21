import { fireEvent, screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { ADMIN_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { AdminCourseReviews } from "./AdminCourseReviews";

const PENDING_TITLE = "JOIN trong thực tế";

async function openPendingCourse() {
    renderWithProviders(
        <ToastProvider>
            <AdminCourseReviews />
        </ToastProvider>,
    );
    const queue = within(await screen.findByRole("region", { name: "Khoá chờ duyệt" }));
    fireEvent.click(await queue.findByRole("button", { name: new RegExp(PENDING_TITLE) }));
    return within(await screen.findByRole("region", { name: "Chi tiết khoá" }));
}

describe("duyệt khoá học", () => {
    beforeEach(() => {
        writeMockSession(ADMIN_SESSION_USER);
    });

    it("liệt kê khoá chờ duyệt và hiện đề cương của khoá đang chọn", async () => {
        const detail = await openPendingCourse();

        expect(await detail.findByRole("heading", { name: PENDING_TITLE })).toBeInTheDocument();
        expect(await detail.findByText("1. Ghép hai bảng")).toBeInTheDocument();
        expect(detail.getByRole("link", { name: "INNER JOIN" })).toHaveAttribute(
            "href",
            "/studio/courses/0192f3f1-0000-7000-8000-000000000002/lessons/0192f3f3-0000-7000-8000-000000000005",
        );
    });

    it("duyệt và xuất bản thì khoá rời khỏi hàng chờ", async () => {
        const detail = await openPendingCourse();

        fireEvent.click(await detail.findByRole("button", { name: "Duyệt và xuất bản" }));

        expect(await screen.findByText(`Đã xuất bản khoá ${PENDING_TITLE}.`)).toBeInTheDocument();
        expect(await screen.findByText("Không có khoá nào chờ duyệt.")).toBeInTheDocument();
    });

    it("yêu cầu sửa bắt buộc có lý do", async () => {
        const detail = await openPendingCourse();

        fireEvent.click(await detail.findByRole("button", { name: "Yêu cầu sửa" }));
        expect(await detail.findByText("Cần ghi lý do")).toBeInTheDocument();

        fireEvent.change(detail.getByRole("textbox", { name: "Lý do gửi người soạn" }), {
            target: { value: "Bổ sung bài về FULL JOIN." },
        });
        fireEvent.click(detail.getByRole("button", { name: "Yêu cầu sửa" }));

        expect(
            await screen.findByText(`Đã trả khoá ${PENDING_TITLE} về cho người soạn.`),
        ).toBeInTheDocument();
        expect(await screen.findByText("Không có khoá nào chờ duyệt.")).toBeInTheDocument();
    });
});
