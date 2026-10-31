import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type {
    ProblemStatus,
    StatusCounts,
    StudioProblem,
    StudioProblemSummary,
    UpdateProblemRequest,
} from "@/features/studio/types";
import type { Difficulty } from "@/lib/difficulty";
import type { Page } from "@/lib/pagination";

import { validationFailed } from "./admin-user-errors";
import { apiError } from "./api-error";
import { PRACTICE_TAGS } from "./practice-fixtures";
import { readMockSession, SETTER_SESSION_USER } from "./session";

const STORAGE_KEY = "sqljudge:mock:studio-problems";
const HOUR_MS = 60 * 60_000;
const DEFAULT_PAGE_SIZE = 20;
const MAX_TITLE_LENGTH = 200;
const FIRST_SLUG_SUFFIX = 2;
const SLUG_PATTERN = /^[a-z0-9]+(-[a-z0-9]+)*$/;
const STATUSES: readonly ProblemStatus[] = [
    "DRAFT",
    "PENDING_REVIEW",
    "CHANGES_REQUESTED",
    "REJECTED",
    "PUBLISHED",
];

interface StoredProblem extends StudioProblemSummary {
    authorId: string;
    tagSlugs: string[];
    statementMarkdown: string;
}

type ProblemSeed = [
    title: string,
    slug: string,
    status: ProblemStatus,
    difficulty: Difficulty,
    number: number | null,
    hoursAgo: number,
];

const SEEDS: ProblemSeed[] = [
    ["Phễu chuyển đổi 4 bước", "phieu-chuyen-doi-4-buoc", "DRAFT", "MEDIUM", null, 2],
    [
        "Chuỗi ngày hoạt động dài nhất",
        "longest-activity-streak",
        "CHANGES_REQUESTED",
        "HARD",
        null,
        26,
    ],
    ["Lương trung vị mỗi phòng ban", "median-salary-per-department", "DRAFT", "MEDIUM", null, 50],
    [
        "Tỉ lệ giữ chân theo cohort tháng",
        "monthly-cohort-retention",
        "PENDING_REVIEW",
        "HARD",
        null,
        72,
    ],
    ["Đơn hàng giao trễ theo vùng", "late-orders-by-region", "PUBLISHED", "MEDIUM", 260, 120],
    ["Cặp sinh viên cùng lớp", "classmate-pairs", "PUBLISHED", "MEDIUM", 41, 400],
    [
        "Nhân viên lương cao hơn quản lý",
        "employees-earning-more-than-managers",
        "PUBLISHED",
        "EASY",
        18,
        900,
    ],
    ["Khoảng trống trong lịch đặt phòng", "booking-gaps", "PUBLISHED", "HARD", 117, 1300],
    ["Sản phẩm bán chạy theo quý", "best-sellers-per-quarter", "PUBLISHED", "MEDIUM", 52, 1700],
];

function seedProblems(): StoredProblem[] {
    return SEEDS.map(([title, slug, status, difficulty, number, hoursAgo], index) => ({
        id: `0192f3d0-0000-7000-8000-${String(index + 1).padStart(12, "0")}`,
        authorId: SETTER_SESSION_USER.id,
        number,
        slug,
        title,
        status,
        difficulty,
        updatedAt: new Date(Date.now() - hoursAgo * HOUR_MS).toISOString(),
        tagSlugs: [],
        statementMarkdown: "",
    }));
}

export function readStudioProblems(): StoredProblem[] {
    const stored = sessionStorage.getItem(STORAGE_KEY);
    if (!stored) {
        return seedProblems();
    }
    return (JSON.parse(stored) as StoredProblem[]).map((problem) => ({
        ...problem,
        tagSlugs: problem.tagSlugs ?? [],
        statementMarkdown: problem.statementMarkdown ?? "",
    }));
}

export function writeStudioProblems(problems: StoredProblem[]): void {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(problems));
}

function folded(text: string): string {
    return text
        .normalize("NFD")
        .replace(/\p{M}/gu, "")
        .replace(/đ/g, "d")
        .replace(/Đ/g, "D")
        .toLowerCase();
}

function toSummary(problem: StoredProblem): StudioProblemSummary {
    return {
        id: problem.id,
        number: problem.number,
        slug: problem.slug,
        title: problem.title,
        status: problem.status,
        difficulty: problem.difficulty,
        updatedAt: problem.updatedAt,
    };
}

function findAccessible(problemId: string): StoredProblem | undefined {
    const user = readMockSession();
    return readStudioProblems().find(
        (problem) =>
            problem.id === problemId && (problem.authorId === user?.id || user?.role === "ADMIN"),
    );
}

function toDetail(problem: StoredProblem): StudioProblem {
    return {
        ...toSummary(problem),
        tags: PRACTICE_TAGS.filter((tag) => problem.tagSlugs.includes(tag.slug)),
        statementMarkdown: problem.statementMarkdown,
    };
}

function ownProblems(userId: string): StoredProblem[] {
    return readStudioProblems()
        .filter((problem) => problem.authorId === userId)
        .sort((first, second) => Date.parse(second.updatedAt) - Date.parse(first.updatedAt));
}

function uniqueSlug(title: string, problems: StoredProblem[]): string {
    const base =
        folded(title)
            .replace(/[^a-z0-9]+/g, "-")
            .replace(/^-+|-+$/g, "") || "bai-tap";
    const taken = new Set(problems.map((problem) => problem.slug));
    let slug = base;
    for (let suffix = FIRST_SLUG_SUFFIX; taken.has(slug); suffix += 1) {
        slug = `${base}-${suffix}`;
    }
    return slug;
}

export const studioProblemHandlers: RequestHandler[] = [
    http.post("*/api/studio/problems", async ({ request }) => {
        await delay();
        const user = readMockSession();
        if (!user) {
            return apiError(401, "UNAUTHENTICATED", "Bạn chưa đăng nhập");
        }
        const title = ((await request.json()) as { title?: string }).title?.trim() ?? "";
        if (title === "" || title.length > MAX_TITLE_LENGTH) {
            return validationFailed({
                field: "title",
                message: title === "" ? "Không được để trống" : "Tối đa 200 ký tự",
            });
        }
        const problems = readStudioProblems();
        const created: StoredProblem = {
            id: crypto.randomUUID(),
            authorId: user.id,
            number: null,
            slug: uniqueSlug(title, problems),
            title,
            status: "DRAFT",
            difficulty: "MEDIUM",
            updatedAt: new Date().toISOString(),
            tagSlugs: [],
            statementMarkdown: "",
        };
        writeStudioProblems([...problems, created]);
        return HttpResponse.json(toSummary(created), {
            status: 201,
            headers: { Location: `/api/studio/problems/${created.id}` },
        });
    }),

    http.get("*/api/studio/problems/status-counts", async () => {
        await delay();
        const user = readMockSession();
        if (!user) {
            return apiError(401, "UNAUTHENTICATED", "Bạn chưa đăng nhập");
        }
        const counts = Object.fromEntries(STATUSES.map((status) => [status, 0])) as StatusCounts;
        ownProblems(user.id).forEach((problem) => {
            counts[problem.status] += 1;
        });
        return HttpResponse.json(counts);
    }),

    http.get("*/api/studio/problems/:problemId", async ({ params }) => {
        await delay();
        const problem = findAccessible(String(params.problemId));
        return problem
            ? HttpResponse.json(toDetail(problem))
            : apiError(404, "PROBLEM_NOT_FOUND", "Không tìm thấy bài tập");
    }),

    http.patch("*/api/studio/problems/:problemId", async ({ params, request }) => {
        await delay();
        const problem = findAccessible(String(params.problemId));
        if (!problem) {
            return apiError(404, "PROBLEM_NOT_FOUND", "Không tìm thấy bài tập");
        }
        if (problem.status !== "DRAFT" && problem.status !== "CHANGES_REQUESTED") {
            return apiError(422, "PROBLEM_LOCKED", "Bài đang chờ duyệt hoặc đã xuất bản");
        }
        const change = (await request.json()) as UpdateProblemRequest;
        const problems = readStudioProblems();
        if (change.slug !== undefined && !SLUG_PATTERN.test(change.slug)) {
            return validationFailed({ field: "slug", message: "Slug không hợp lệ" });
        }
        if (problems.some((other) => other.slug === change.slug && other.id !== problem.id)) {
            return apiError(409, "SLUG_TAKEN", "Slug đã được bài khác dùng");
        }
        if (change.tagSlugs?.some((slug) => !PRACTICE_TAGS.some((tag) => tag.slug === slug))) {
            return validationFailed({ field: "tagSlugs", message: "Có chủ đề không tồn tại" });
        }
        const updated: StoredProblem = {
            ...problem,
            ...change,
            updatedAt: new Date().toISOString(),
        };
        writeStudioProblems(problems.map((other) => (other.id === problem.id ? updated : other)));
        return HttpResponse.json(toDetail(updated));
    }),

    http.get("*/api/studio/problems", async ({ request }) => {
        await delay();
        const user = readMockSession();
        if (!user) {
            return apiError(401, "UNAUTHENTICATED", "Bạn chưa đăng nhập");
        }
        const query = new URL(request.url).searchParams;
        const status = query.get("status");
        const keyword = folded(query.get("q")?.trim() ?? "");
        const page = Number(query.get("page") ?? 1);
        const size = Number(query.get("size") ?? DEFAULT_PAGE_SIZE);
        const matches = ownProblems(user.id).filter(
            (problem) =>
                (status === null || problem.status === status) &&
                folded(problem.title).includes(keyword),
        );
        const response: Page<StudioProblemSummary> = {
            items: matches.slice((page - 1) * size, page * size).map(toSummary),
            page,
            size,
            totalItems: matches.length,
            totalPages: Math.ceil(matches.length / size),
        };
        return HttpResponse.json(response);
    }),
];
