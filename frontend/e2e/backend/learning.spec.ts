import { expect, test } from "@playwright/test";

import { logIn, registerVerifiedLearner } from "./support/learner";

const FIRST_LESSON = "/learn/sql-co-ban/select-la-gi";
const SECOND_LESSON = "/learn/sql-co-ban/chon-cot";

test.describe("học khoá SQL cơ bản với backend thật", () => {
    test.setTimeout(180_000);

    test("chạy ô SQL trong sandbox, làm quiz rồi hoàn thành bài", async ({ page, request }) => {
        const learner = await registerVerifiedLearner(request);
        await logIn(page, learner, FIRST_LESSON);

        const cell = page.getByRole("region", { name: "Ô SQL" });
        await cell.getByRole("button", { name: "Chạy" }).click();
        const result = cell.getByRole("table", { name: "Kết quả" });
        await expect(result).toBeVisible({ timeout: 90_000 });
        await expect(result.getByRole("row")).toHaveCount(4);
        await expect(result.getByRole("cell", { name: "Kỹ thuật" })).toBeVisible();

        const quiz = page.getByRole("form", { name: "Câu hỏi" });
        await quiz.getByRole("radio", { name: "Lấy mọi bảng trong database" }).check();
        await quiz.getByRole("button", { name: "Nộp bài" }).click();
        await expect(page.getByText("Chưa đúng")).toBeVisible();

        await quiz.getByRole("radio", { name: "Lấy mọi cột của bảng" }).check();
        await quiz.getByRole("button", { name: "Nộp bài" }).click();
        await expect(page.getByRole("main").getByText("Đã hoàn thành")).toBeVisible();

        await page.goto(SECOND_LESSON);
        await page.getByRole("button", { name: "Hoàn thành bài học" }).click();
        await expect(page.getByRole("main").getByText("Đã hoàn thành")).toBeVisible();
    });
});
