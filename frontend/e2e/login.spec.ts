import { expect, test } from "@playwright/test";

import { fillLoginForm, MOCK_LEARNER } from "./support/auth";

test.describe("đăng nhập từ trang giới thiệu", () => {
    test("học viên đi từ landing tới đăng nhập và thấy tài khoản trên thanh trên cùng", async ({
        page,
    }) => {
        await page.goto("/");
        await page.getByRole("link", { name: "Đăng nhập" }).click();
        await expect(page).toHaveURL("/login", { timeout: 30_000 });

        await fillLoginForm(page, MOCK_LEARNER.email, MOCK_LEARNER.password);
        await expect(page).toHaveURL("/");

        await page.goto(`/u/${MOCK_LEARNER.username}`);
        const accountButton = page.getByRole("button", { name: "Tài khoản của tôi" });
        await expect(accountButton).toBeVisible();
        const pageWidth = await page.evaluate(() => document.documentElement.scrollWidth);
        expect(pageWidth).toBeLessThanOrEqual(page.viewportSize()?.width ?? pageWidth);
        await accountButton.click();
        await expect(page.getByRole("menuitem", { name: "Hồ sơ của tôi" })).toBeVisible();
        await expect(page.getByRole("menuitem", { name: "Đăng xuất" })).toBeVisible();
    });

    test("báo lỗi khi sai mật khẩu và giữ người dùng ở trang đăng nhập", async ({ page }) => {
        await page.goto("/login");

        await fillLoginForm(page, MOCK_LEARNER.email, "wrong-password");

        await expect(page.getByText("Email hoặc mật khẩu không đúng")).toBeVisible();
        await expect(page).toHaveURL("/login");
    });
});
