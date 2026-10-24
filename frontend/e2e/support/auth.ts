import type { Page } from "@playwright/test";

export const MOCK_LEARNER = {
    email: "demo@sqljudge.dev",
    password: "mock-password",
    username: "minh.anh",
};

export async function fillLoginForm(page: Page, email: string, password: string): Promise<void> {
    await page.getByLabel("Email").fill(email);
    await page.getByLabel("Mật khẩu", { exact: true }).fill(password);
    await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
}
