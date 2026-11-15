import { fireEvent, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { SETTER_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { CourseStudio } from "./CourseStudio";

const DRAFT_COURSE_ID = "0192f3f1-0000-7000-8000-000000000001";
const IN_REVIEW_COURSE_ID = "0192f3f1-0000-7000-8000-000000000002";

function renderCourseStudio(courseId: string) {
    renderWithProviders(
        <ToastProvider>
            <CourseStudio courseId={courseId} />
        </ToastProvider>,
    );
}

describe("trang soạn khoá học", () => {
    beforeEach(() => {
        writeMockSession(SETTER_SESSION_USER);
    });

    it("sửa và lưu thông tin khoá nháp", async () => {
        renderCourseStudio(DRAFT_COURSE_ID);

        const title = await screen.findByRole("textbox", { name: "Tên khoá" });
        expect(title).toHaveValue("Window function từ đầu");
        expect(screen.getByText("4 bài giảng, 90 phút")).toBeInTheDocument();
        expect(screen.queryByRole("spinbutton")).not.toBeInTheDocument();
        fireEvent.change(title, { target: { value: "Window function thực chiến" } });
        fireEvent.click(screen.getByRole("button", { name: "Lưu thông tin" }));

        expect(await screen.findByText("Đã lưu thông tin khoá.")).toBeInTheDocument();
        expect(
            await screen.findByRole("heading", { name: "Window function thực chiến" }),
        ).toBeInTheDocument();
    });

    it("báo lỗi ngay khi tóm tắt trống", async () => {
        renderCourseStudio(DRAFT_COURSE_ID);

        fireEvent.change(await screen.findByRole("textbox", { name: "Tóm tắt" }), {
            target: { value: " " },
        });
        fireEvent.click(screen.getByRole("button", { name: "Lưu thông tin" }));

        expect(await screen.findByText("Không được để trống")).toBeInTheDocument();
    });

    it("khoá đang chờ duyệt thì chỉ xem, không sửa được", async () => {
        renderCourseStudio(IN_REVIEW_COURSE_ID);

        expect(await screen.findByRole("textbox", { name: "Tên khoá" })).toBeDisabled();
        expect(screen.getByText("Khoá đang chờ duyệt nên chưa sửa được.")).toBeInTheDocument();
        expect(screen.queryByRole("button", { name: "Lưu thông tin" })).not.toBeInTheDocument();
    });

    it("báo không tìm thấy khi khoá không thuộc người ra đề", async () => {
        renderCourseStudio("0192f3f1-0000-7000-8000-0000000000ff");

        expect(await screen.findByText("Không tìm thấy khoá học")).toBeInTheDocument();
    });
});
