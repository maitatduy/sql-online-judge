import { fireEvent, screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { SETTER_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { CourseStudio } from "./CourseStudio";

const DRAFT_COURSE_ID = "0192f3f1-0000-7000-8000-000000000001";
const IN_REVIEW_COURSE_ID = "0192f3f1-0000-7000-8000-000000000002";

async function renderOutline(courseId = DRAFT_COURSE_ID) {
    renderWithProviders(
        <ToastProvider>
            <CourseStudio courseId={courseId} />
        </ToastProvider>,
    );
    return within(await screen.findByRole("region", { name: "Đề cương" }));
}

async function chapterCard(outline: ReturnType<typeof within>, title: string) {
    return within(await outline.findByRole("group", { name: title }));
}

describe("đề cương khoá trong Studio", () => {
    beforeEach(() => {
        writeMockSession(SETTER_SESSION_USER);
    });

    it("liệt kê chương theo thứ tự, mỗi bài kèm thời lượng", async () => {
        const outline = await renderOutline();

        const chapter = await chapterCard(outline, "Xếp hạng");
        expect(chapter.getByText("ROW_NUMBER và RANK")).toBeInTheDocument();
        expect(chapter.getAllByRole("listitem").map((item) => item.textContent)).toEqual([
            "ROW_NUMBER và RANKrow-number-va-rank20 phút",
            "DENSE_RANKdense-rank20 phút",
        ]);
        expect(
            outline.getAllByRole("group").map((group) => group.getAttribute("aria-label")),
        ).toEqual(["Xếp hạng", "Cộng dồn"]);
    });

    it("thêm chương mới vào cuối đề cương", async () => {
        const outline = await renderOutline();

        fireEvent.change(await outline.findByRole("textbox", { name: "Tên chương mới" }), {
            target: { value: "So sánh với dòng trước" },
        });
        fireEvent.click(outline.getByRole("button", { name: "Thêm chương" }));

        expect(
            await outline.findByRole("group", { name: "So sánh với dòng trước" }),
        ).toBeInTheDocument();
    });

    it("thêm bài giảng, slug tự sinh từ tên và tổng thời lượng khoá cập nhật", async () => {
        const outline = await renderOutline();
        const chapter = await chapterCard(outline, "Cộng dồn");

        fireEvent.click(chapter.getByRole("button", { name: "Thêm bài giảng" }));
        fireEvent.change(chapter.getByRole("textbox", { name: "Tên bài giảng" }), {
            target: { value: "Tổng lũy kế theo tháng" },
        });
        expect(chapter.getByRole("textbox", { name: "Slug" })).toHaveValue(
            "tong-luy-ke-theo-thang",
        );
        fireEvent.change(chapter.getByRole("spinbutton", { name: "Thời lượng (phút)" }), {
            target: { value: "15" },
        });
        fireEvent.click(chapter.getByRole("button", { name: "Lưu bài giảng" }));

        expect(await chapter.findByText("Tổng lũy kế theo tháng")).toBeInTheDocument();
        expect(await screen.findByText("5 bài giảng, 105 phút")).toBeInTheDocument();
    });

    it("báo lỗi ở ô slug khi bài giảng khác đã dùng slug đó", async () => {
        const outline = await renderOutline();
        const chapter = await chapterCard(outline, "Cộng dồn");

        fireEvent.click(chapter.getByRole("button", { name: "Thêm bài giảng" }));
        fireEvent.change(chapter.getByRole("textbox", { name: "Tên bài giảng" }), {
            target: { value: "Row number và rank" },
        });
        fireEvent.change(chapter.getByRole("textbox", { name: "Slug" }), {
            target: { value: "row-number-va-rank" },
        });
        fireEvent.change(chapter.getByRole("spinbutton", { name: "Thời lượng (phút)" }), {
            target: { value: "10" },
        });
        fireEvent.click(chapter.getByRole("button", { name: "Lưu bài giảng" }));

        expect(
            await chapter.findByText("Slug đã được bài giảng khác trong khoá dùng"),
        ).toBeInTheDocument();
    });

    it("đổi tên chương", async () => {
        const outline = await renderOutline();
        const chapter = await chapterCard(outline, "Xếp hạng");

        fireEvent.click(chapter.getByRole("button", { name: "Đổi tên chương Xếp hạng" }));
        fireEvent.change(chapter.getByRole("textbox", { name: "Tên chương" }), {
            target: { value: "Hàm xếp hạng" },
        });
        fireEvent.click(chapter.getByRole("button", { name: "Lưu tên" }));

        expect(await outline.findByRole("group", { name: "Hàm xếp hạng" })).toBeInTheDocument();
    });

    it("xoá bài giảng sau khi xác nhận, chương còn bài thì không xoá được", async () => {
        const outline = await renderOutline();
        const chapter = await chapterCard(outline, "Xếp hạng");

        fireEvent.click(chapter.getByRole("button", { name: "Xoá bài giảng DENSE_RANK" }));
        const confirm = within(await screen.findByRole("alertdialog"));
        fireEvent.click(confirm.getByRole("button", { name: "Xoá bài giảng" }));

        expect(await screen.findByText("3 bài giảng, 70 phút")).toBeInTheDocument();
        expect(chapter.queryByText("DENSE_RANK")).not.toBeInTheDocument();

        fireEvent.click(chapter.getByRole("button", { name: "Xoá chương Xếp hạng" }));
        fireEvent.click(
            within(await screen.findByRole("alertdialog")).getByRole("button", {
                name: "Xoá chương",
            }),
        );
        expect(
            await screen.findByText("Chuyển hoặc xoá bài giảng trước khi xoá chương"),
        ).toBeInTheDocument();
    });

    it("khoá đang chờ duyệt chỉ hiện đề cương, không có nút sửa", async () => {
        const outline = await renderOutline(IN_REVIEW_COURSE_ID);

        await outline.findByRole("group", { name: "Ghép hai bảng" });
        expect(outline.queryByRole("button", { name: "Thêm chương" })).not.toBeInTheDocument();
        expect(outline.queryByRole("button", { name: "Thêm bài giảng" })).not.toBeInTheDocument();
    });
});
