import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type { ProblemStatus, StatusCounts, StudioProblemSummary } from "@/features/studio/types";
import type { Difficulty } from "@/lib/difficulty";
import type { Page } from "@/lib/pagination";

import { apiError } from "./api-error";
import { readMockSession, SETTER_SESSION_USER } from "./session";

const STORAGE_KEY = "sqljudge:mock:studio-problems";
const HOUR_MS = 60 * 60_000;
const DEFAULT_PAGE_SIZE = 20;
const STATUSES: readonly ProblemStatus[] = [
    "DRAFT",
    "PENDING_REVIEW",
    "CHANGES_REQUESTED",
    "REJECTED",
    "PUBLISHED",
];

interface StoredProblem extends StudioProblemSummary {
    authorId: string;
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
    }));
}

export function readStudioProblems(): StoredProblem[] {
    const stored = sessionStorage.getItem(STORAGE_KEY);
    return stored ? (JSON.parse(stored) as StoredProblem[]) : seedProblems();
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

function ownProblems(userId: string): StoredProblem[] {
    return readStudioProblems()
        .filter((problem) => problem.authorId === userId)
        .sort((first, second) => Date.parse(second.updatedAt) - Date.parse(first.updatedAt));
}

export const studioProblemHandlers: RequestHandler[] = [
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
