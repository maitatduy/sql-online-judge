import type {
    AdminUserDetail,
    AdminUserSummary,
    SignupProvider,
    ViolationSeverity,
} from "@/features/admin/users/types";

interface ViolationSeed {
    severity: ViolationSeverity;
    title: string;
    detail: string;
}

interface DetailSeed {
    displayName?: string;
    signupProvider?: SignupProvider;
    violations?: ViolationSeed[];
}

const DETAIL_SEEDS: Record<string, DetailSeed> = {
    "quang.le": { displayName: "Lê Minh Quang" },
    "trang.hoang": { displayName: "Hoàng Thu Trang", signupProvider: "GOOGLE" },
    datahust: { displayName: "CLB Data HUST" },
    "minh.anh": { displayName: "Nguyễn Minh Anh", signupProvider: "GOOGLE" },
    "viet.luu": {
        displayName: "Lưu Quốc Việt",
        signupProvider: "GITHUB",
        violations: [
            {
                severity: "SEVERE",
                title: "Nghi đạo code trong contest",
                detail: "Weekly #37, đang chờ xử lý",
            },
            { severity: "MINOR", title: "Spam bình luận", detail: "12/09, mod.thu đã cảnh cáo" },
        ],
    },
    "spam.bot88": {
        violations: [
            { severity: "SEVERE", title: "Tài khoản spam", detail: "11/10, khoá bởi mod.thu" },
        ],
    },
    "hoa.kieu": { displayName: "Kiều Thị Hoa" },
    "admin.huy": { displayName: "Trần Quốc Huy" },
    "tam.nguyen": { displayName: "Nguyễn Thành Tâm" },
    "mod.thu": { displayName: "Hà Anh Thư" },
    "copy.paste": {
        signupProvider: "GOOGLE",
        violations: [
            {
                severity: "SEVERE",
                title: "Nộp bài trùng code người khác",
                detail: "Weekly #36, khoá 30 ngày",
            },
        ],
    },
    "an.bui": { displayName: "Bùi Văn An", signupProvider: "GITHUB" },
};

export function buildAdminUserDetail(
    user: AdminUserSummary,
    addedViolations: ViolationSeed[] = [],
): AdminUserDetail {
    const seed = DETAIL_SEEDS[user.username] ?? {};
    return {
        ...user,
        displayName: seed.displayName ?? null,
        signupProvider: seed.signupProvider ?? "EMAIL",
        violations: [...(seed.violations ?? []), ...addedViolations].map((violation, index) => ({
            id: `${user.username}-${index + 1}`,
            ...violation,
        })),
    };
}
