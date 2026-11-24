import { expect, test } from "@playwright/test";

import { fillLoginForm, MOCK_LEARNER } from "./support/auth";

test.describe("cộng đồng", () => {
    test.beforeEach(async ({ page }) => {
        await page.goto("/login");
        await fillLoginForm(page, MOCK_LEARNER.email, MOCK_LEARNER.password);
        await expect(page).toHaveURL("/");
    });

    test("viết bài, đăng rồi bình luận dưới bài vừa đăng", async ({ page }) => {
        test.setTimeout(150_000);
        await page.goto("/community/new");

        await page.getByText("Hỏi đáp", { exact: true }).click();
        await page.getByLabel("Tiêu đề").fill("Khi nào dùng HAVING thay cho WHERE?");
        await page.getByLabel("Nội dung").fill("Mình hay nhầm **HAVING** với WHERE.");
        await page.getByRole("button", { name: "Đăng bài" }).click();

        await expect(
            page.getByRole("heading", { level: 1, name: "Khi nào dùng HAVING thay cho WHERE?" }),
        ).toBeVisible({ timeout: 90_000 });
        await page.getByLabel("Viết bình luận… hỗ trợ Markdown").fill("HAVING lọc sau GROUP BY.");
        await page.getByRole("button", { name: "Gửi" }).click();

        await expect(page.getByText("HAVING lọc sau GROUP BY.")).toBeVisible();
        await expect(page.getByRole("heading", { name: "1 bình luận" })).toBeVisible();
    });

    test("lọc danh sách theo chuyên mục", async ({ page }) => {
        await page.goto("/community?category=INTERVIEW");

        const list = page.getByRole("list", { name: "Bài viết" });
        await expect(list.getByRole("listitem")).toHaveCount(1, { timeout: 30_000 });
        await expect(list).toContainText("Câu hỏi phỏng vấn: lương cao thứ hai");
    });
});
