import { fireEvent, screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { SETTER_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { LessonEditor } from "./LessonEditor";

const DRAFT_COURSE_ID = "0192f3f1-0000-7000-8000-000000000001";
const DRAFT_LESSON_ID = "0192f3f3-0000-7000-8000-000000000001";
const IN_REVIEW_COURSE_ID = "0192f3f1-0000-7000-8000-000000000002";
const IN_REVIEW_LESSON_ID = "0192f3f3-0000-7000-8000-000000000005";

function renderEditor(courseId: string, lessonId: string) {
    renderWithProviders(
        <ToastProvider>
            <LessonEditor courseId={courseId} lessonId={lessonId} />
        </ToastProvider>,
    );
}

describe("trình soạn bài giảng", () => {
    beforeEach(() => {
        writeMockSession(SETTER_SESSION_USER);
    });

    it("soạn nội dung markdown, xem trước rồi lưu", async () => {
        renderEditor(DRAFT_COURSE_ID, DRAFT_LESSON_ID);

        expect(await screen.findByRole("textbox", { name: "Tên bài giảng" })).toHaveValue(
            "ROW_NUMBER và RANK",
        );
        expect(screen.getByRole("link", { name: "Window function từ đầu" })).toHaveAttribute(
            "href",
            `/studio/courses/${DRAFT_COURSE_ID}`,
        );
        fireEvent.change(screen.getByRole("textbox", { name: "Nội dung" }), {
            target: { value: "## Đánh số dòng\n\nDùng `ROW_NUMBER()`." },
        });
        fireEvent.mouseDown(screen.getByRole("tab", { name: "Xem trước" }));
        expect(await screen.findByRole("heading", { name: "Đánh số dòng" })).toBeInTheDocument();

        fireEvent.click(screen.getByRole("button", { name: "Lưu bài giảng" }));

        expect(await screen.findByText("Đã lưu bài giảng.")).toBeInTheDocument();
        expect(screen.getByRole("button", { name: "Lưu bài giảng" })).toBeDisabled();
    });

    it("báo lỗi ở ô slug khi slug trùng bài giảng khác", async () => {
        renderEditor(DRAFT_COURSE_ID, DRAFT_LESSON_ID);

        fireEvent.change(await screen.findByRole("textbox", { name: "Slug" }), {
            target: { value: "dense-rank" },
        });
        fireEvent.click(screen.getByRole("button", { name: "Lưu bài giảng" }));

        expect(
            await screen.findByText("Slug đã được bài giảng khác trong khoá dùng"),
        ).toBeInTheDocument();
    });

    it("báo lỗi ngay khi thời lượng ngoài khoảng cho phép", async () => {
        renderEditor(DRAFT_COURSE_ID, DRAFT_LESSON_ID);

        fireEvent.change(await screen.findByRole("spinbutton", { name: "Thời lượng (phút)" }), {
            target: { value: "0" },
        });
        fireEvent.click(screen.getByRole("button", { name: "Lưu bài giảng" }));

        expect(await screen.findByText("Từ 1 đến 600 phút")).toBeInTheDocument();
    });

    it("khoá đang chờ duyệt thì chỉ xem bài giảng", async () => {
        renderEditor(IN_REVIEW_COURSE_ID, IN_REVIEW_LESSON_ID);

        expect(await screen.findByRole("textbox", { name: "Tên bài giảng" })).toBeDisabled();
        expect(screen.getByText("Khoá đang chờ duyệt nên chưa sửa được.")).toBeInTheDocument();
        expect(screen.queryByRole("button", { name: "Lưu bài giảng" })).not.toBeInTheDocument();
    });

    it("báo không tìm thấy khi bài giảng không thuộc người ra đề", async () => {
        renderEditor(DRAFT_COURSE_ID, "0192f3f3-0000-7000-8000-0000000000ff");

        expect(await screen.findByText("Không tìm thấy bài giảng")).toBeInTheDocument();
    });

    it("tên bài trong đề cương mở trình soạn bài giảng", async () => {
        const { CourseStudio } = await import("./CourseStudio");
        renderWithProviders(
            <ToastProvider>
                <CourseStudio courseId={DRAFT_COURSE_ID} />
            </ToastProvider>,
        );

        const outline = within(await screen.findByRole("region", { name: "Đề cương" }));
        expect(await outline.findByRole("link", { name: "DENSE_RANK" })).toHaveAttribute(
            "href",
            `/studio/courses/${DRAFT_COURSE_ID}/lessons/0192f3f3-0000-7000-8000-000000000002`,
        );
    });
});
