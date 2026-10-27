import { describe, expect, it } from "vitest";

import { ineligibilityOf, isInvitableEmail } from "./eligibility";

const learner = { email: "viet.luu@example.com", role: "LEARNER", status: "ACTIVE" } as const;

describe("xét người có mời được không", () => {
    it("học viên đang hoạt động, chưa có lời mời thì mời được", () => {
        expect(ineligibilityOf(learner, "SETTER", new Set())).toBeNull();
    });

    it("trả lý do khi là quản trị, đã có vai trò, bị khoá hoặc đang chờ lời mời", () => {
        expect(ineligibilityOf({ ...learner, role: "ADMIN" }, "SETTER", new Set())).toBe(
            "Là quản trị viên",
        );
        expect(ineligibilityOf({ ...learner, role: "SETTER" }, "SETTER", new Set())).toBe(
            "Đã là Người ra đề",
        );
        expect(ineligibilityOf({ ...learner, role: "MODERATOR" }, "MODERATOR", new Set())).toBe(
            "Đã là Kiểm duyệt",
        );
        expect(ineligibilityOf({ ...learner, status: "BANNED" }, "SETTER", new Set())).toBe(
            "Tài khoản đang bị khoá",
        );
        expect(ineligibilityOf(learner, "SETTER", new Set(["viet.luu@example.com"]))).toBe(
            "Đang chờ nhận lời mời",
        );
    });

    it("đổi vai trò đang mời thì xét lại: Người ra đề vẫn mời làm Kiểm duyệt được", () => {
        expect(ineligibilityOf({ ...learner, role: "SETTER" }, "MODERATOR", new Set())).toBeNull();
    });

    it("chỉ nhận chuỗi đúng dạng email", () => {
        expect(isInvitableEmail(" Moi.Ban@Gmail.com ")).toBe(true);
        expect(isInvitableEmail("moi.ban")).toBe(false);
        expect(isInvitableEmail("a@b")).toBe(false);
    });
});
