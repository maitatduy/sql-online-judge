import type {
    AdminUserDetail,
    AdminUserSummary,
    SignupProvider,
    ViolationSeverity,
    ViolationStatus,
} from "@/features/admin/users/types";

const SUBMISSIONS_PER_ACCEPTED = 3;
const RATING_PER_CONTEST = 150;

interface ViolationSeed {
    severity: ViolationSeverity;
    title: string;
    detail: string;
    status: ViolationStatus;
    handledBy: string | null;
    handledAt: string | null;
}

const OPEN_CASE = { status: "OPEN", handledBy: null, handledAt: null } as const;

function resolvedBy(handledBy: string, handledAt: string) {
    return { status: "RESOLVED", handledBy, handledAt } as const;
}

interface DetailSeed {
    displayName?: string;
    school?: string;
    submissionCount?: number;
    contestCount?: number;
    signupProvider?: SignupProvider;
    violations?: ViolationSeed[];
}

const DETAIL_SEEDS: Record<string, DetailSeed> = {
    "quang.le": { displayName: "Lê Minh Quang", school: "ĐH Công nghệ, ĐHQGHN" },
    "trang.hoang": { displayName: "Hoàng Thu Trang", signupProvider: "GOOGLE" },
    datahust: { displayName: "CLB Data HUST" },
    "minh.anh": { displayName: "Nguyễn Minh Anh", signupProvider: "GOOGLE" },
    "viet.luu": {
        displayName: "Lưu Quốc Việt",
        school: "ĐH Mở Hà Nội",
        submissionCount: 286,
        contestCount: 9,
        signupProvider: "GITHUB",
        violations: [
            {
                severity: "SEVERE",
                title: "Nghi đạo code trong contest",
                detail: "Weekly #37, đang chờ xử lý",
                ...OPEN_CASE,
            },
            {
                severity: "MINOR",
                title: "Spam bình luận",
                detail: "12/09, mod.thu đã cảnh cáo",
                ...resolvedBy("mod.thu", "2026-09-12T03:00:00Z"),
            },
        ],
    },
    "spam.bot88": {
        violations: [
            {
                severity: "SEVERE",
                title: "Tài khoản spam",
                detail: "11/10, khoá bởi mod.thu",
                ...resolvedBy("mod.thu", "2026-10-11T03:00:00Z"),
            },
        ],
    },
    "hoa.kieu": { displayName: "Kiều Thị Hoa", school: "ĐH Huế" },
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
                ...resolvedBy("mod.thu", "2026-10-07T03:00:00Z"),
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
        school: seed.school ?? null,
        signupProvider: seed.signupProvider ?? "EMAIL",
        submissionCount: seed.submissionCount ?? user.acceptedCount * SUBMISSIONS_PER_ACCEPTED,
        contestCount:
            seed.contestCount ??
            (user.rating > 0 ? Math.floor(user.rating / RATING_PER_CONTEST) : 0),
        violations: [...(seed.violations ?? []), ...addedViolations].map((violation, index) => ({
            id: `${user.username}-${index + 1}`,
            ...violation,
        })),
    };
}
