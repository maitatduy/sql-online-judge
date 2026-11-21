import { expect, test } from "@playwright/test";

import { logIn, registerVerifiedLearner } from "./support/learner";
import { grantRole } from "./support/roles";

test.describe("soạn khoá và duyệt với backend thật", () => {
    test.setTimeout(240_000);

    test("setter soạn khoá, gửi duyệt, admin duyệt thì khoá hiện ở trang học", async ({
        browser,
        request,
    }) => {
        const stamp = Date.now();
        const title = `Khoá E2E ${stamp}`;
        const setter = await registerVerifiedLearner(request);
        const admin = await registerVerifiedLearner(request);
        grantRole(setter.email, "SETTER");
        grantRole(admin.email, "ADMIN");

        const setterPage = await (await browser.newContext()).newPage();
        await logIn(setterPage, setter, "/studio/courses");
        await setterPage.getByRole("button", { name: "Tạo khoá mới" }).click();
        const dialog = setterPage.getByRole("dialog", { name: "Tạo khoá mới" });
        await dialog.getByLabel("Tên khoá").fill(title);
        await dialog.getByLabel("Slug").fill(`khoa-e2e-${stamp}`);
        await dialog.getByLabel("Tóm tắt").fill("Khoá tạo bởi E2E.");
        await dialog.getByRole("button", { name: "Tạo khoá nháp" }).click();
        await expect(setterPage.getByRole("heading", { level: 1, name: title })).toBeVisible({
            timeout: 60_000,
        });

        const outline = setterPage.getByRole("region", { name: "Đề cương" });
        await outline.getByRole("textbox", { name: "Tên chương mới" }).fill("Làm quen");
        await outline.getByRole("button", { name: "Thêm chương" }).click();
        const chapter = outline.getByRole("group", { name: "Làm quen" });
        await chapter.getByRole("button", { name: "Thêm bài giảng" }).click();
        await chapter.getByLabel("Tên bài giảng").fill("Câu SELECT đầu tiên");
        await chapter.getByLabel("Thời lượng (phút)").fill("15");
        await chapter.getByRole("button", { name: "Lưu bài giảng" }).click();
        await expect(setterPage.getByText("1 bài giảng, 15 phút")).toBeVisible();

        await setterPage.getByRole("button", { name: "Gửi duyệt" }).click();
        await expect(setterPage.getByText(/Khoá đang chờ admin duyệt/)).toBeVisible();

        const adminPage = await (await browser.newContext()).newPage();
        await logIn(adminPage, admin, "/admin/course-reviews");
        const queue = adminPage.getByRole("region", { name: "Khoá chờ duyệt" });
        await queue.getByRole("button", { name: new RegExp(title) }).click();
        const detail = adminPage.getByRole("region", { name: "Chi tiết khoá" });
        await expect(detail.getByRole("link", { name: "Câu SELECT đầu tiên" })).toBeVisible();
        await detail.getByRole("button", { name: "Duyệt và xuất bản" }).click();
        await expect(
            adminPage.getByText(`Đã xuất bản khoá ${title}.`, { exact: true }),
        ).toBeVisible();

        const visitorPage = await (await browser.newContext()).newPage();
        await visitorPage.goto("/learn");
        await expect(visitorPage.getByRole("link", { name: new RegExp(title) })).toBeVisible({
            timeout: 30_000,
        });
    });
});
