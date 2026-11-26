import { expect, test } from "@playwright/test";

import { logIn, registerVerifiedLearner } from "./support/learner";

const FIRST_LESSON = "/learn/sql-truy-van-co-ban/database-bang-dong-cot";

test.describe("học khoá SQL 1 theo lộ trình với backend thật", () => {
    test.setTimeout(180_000);

    test("chạy ô SQL trong sandbox, làm quiz rồi hoàn thành bài", async ({ page, request }) => {
        const learner = await registerVerifiedLearner(request);
        await logIn(page, learner, FIRST_LESSON);

        const cell = page.getByRole("region", { name: "Ô SQL" });
        await cell.getByRole("button", { name: "Chạy" }).click();
        const result = cell.getByRole("table", { name: "Kết quả" });
        await expect(result).toBeVisible({ timeout: 90_000 });
        await expect(result.getByRole("row")).toHaveCount(16);
        await expect(result.getByRole("cell", { name: "Nguyễn An" })).toBeVisible();

        const quiz = page.getByRole("form", { name: "Câu hỏi" });
        await quiz.getByRole("radio", { name: "Một thuộc tính, ví dụ tên khách hàng" }).check();
        await quiz.getByRole("button", { name: "Nộp bài" }).click();
        await expect(quiz.getByText("Chưa đúng").first()).toBeVisible();

        await quiz.getByRole("radio", { name: "Một bản ghi, ví dụ một khách hàng" }).check();
        await quiz.getByRole("radio", { name: "Khoá ngoại trỏ tới id của bảng customers" }).check();
        await quiz.getByRole("button", { name: "Nộp bài" }).click();
        await expect(page.getByRole("main").getByText("Đã hoàn thành")).toBeVisible();

        await page.reload();
        await expect(
            quiz.getByRole("radio", { name: "Một bản ghi, ví dụ một khách hàng" }),
        ).toBeChecked();
        await expect(quiz.getByText("Đã trả lời đúng tất cả")).toBeVisible();
    });
});
