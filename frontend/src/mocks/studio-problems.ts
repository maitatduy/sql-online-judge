import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type {
    ProblemAnswer,
    ProblemReview,
    ProblemStatus,
    SaveProblemAnswerRequest,
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
import { datasetNotReadyReason, readStudioDatasets } from "./studio-datasets";
import { folded } from "./text";

const STORAGE_KEY = "sqljudge:mock:studio-problems";
const HOUR_MS = 60 * 60_000;
const DEFAULT_PAGE_SIZE = 20;
const MAX_TITLE_LENGTH = 200;
const FIRST_SLUG_SUFFIX = 2;
const SLUG_PATTERN = /^[a-z0-9]+(-[a-z0-9]+)*$/;
export interface StoredProblem extends StudioProblemSummary {
    authorId: string;
    deletedAt?: string;
    tagSlugs: string[];
    statementMarkdown: string;
    datasetId?: string;
    answer?: SaveProblemAnswerRequest;
    latestReview?: ProblemReview;
}

const COMPANY_DATASET_ID = "0192f3e0-0000-7000-8000-000000000001";
const SEEDED_DATASETS: Record<string, string> = {
    "median-salary-per-department": COMPANY_DATASET_ID,
    "monthly-cohort-retention": COMPANY_DATASET_ID,
};
const DEFAULT_FLOAT_TOLERANCE = 0.000001;

function answerOf(problem: StoredProblem): ProblemAnswer {
    const variants =
        readStudioDatasets()
            .find((dataset) => dataset.id === problem.datasetId)
            ?.variants.map((variant) => ({
                id: variant.id,
                name: variant.name,
                isPublic: variant.isPublic,
            })) ?? [];
    const answer = problem.answer;
    return {
        solutions: answer?.solutions ?? [],
        testcases: (answer?.testcaseVariantIds ?? []).flatMap((variantId) =>
            variants
                .filter((variant) => variant.id === variantId)
                .map((variant) => ({
                    variantId: variant.id,
                    variantName: variant.name,
                    isPublic: variant.isPublic,
                })),
        ),
        variants,
        requiresOrder: answer?.requiresOrder ?? false,
        compareColumnNames: answer?.compareColumnNames ?? false,
        floatTolerance: answer?.floatTolerance ?? DEFAULT_FLOAT_TOLERANCE,
    };
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
        datasetId: SEEDED_DATASETS[slug],
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

export function findAccessibleStudioProblem(problemId: string): StoredProblem | undefined {
    const user = readMockSession();
    return readStudioProblems().find(
        (problem) =>
            problem.id === problemId &&
            !problem.deletedAt &&
            (problem.authorId === user?.id || user?.role === "ADMIN"),
    );
}

export function toStudioProblemDetail(problem: StoredProblem): StudioProblem {
    return {
        ...toSummary(problem),
        tags: PRACTICE_TAGS.filter((tag) => problem.tagSlugs.includes(tag.slug)),
        statementMarkdown: problem.statementMarkdown,
        dataset: datasetOf(problem.datasetId),
        latestReview: problem.latestReview ?? null,
    };
}

function datasetOf(datasetId: string | undefined): StudioProblem["dataset"] {
    const dataset = readStudioDatasets().find((candidate) => candidate.id === datasetId);
    return dataset ? { id: dataset.id, name: dataset.name } : null;
}

export function isEditable(problem: StoredProblem): boolean {
    return problem.status === "DRAFT" || problem.status === "CHANGES_REQUESTED";
}

function saveProblem(changed: StoredProblem): void {
    writeStudioProblems(
        readStudioProblems().map((problem) => (problem.id === changed.id ? changed : problem)),
    );
}

function sortedProblems(problems: StoredProblem[], sort: string): StoredProblem[] {
    if (sort === "title,asc") {
        return [...problems].sort((first, second) => first.title.localeCompare(second.title, "vi"));
    }
    return sort === "updatedAt,asc" ? [...problems].reverse() : problems;
}

function ownProblems(userId: string): StoredProblem[] {
    return readStudioProblems()
        .filter((problem) => problem.authorId === userId && !problem.deletedAt)
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

    http.get("*/api/studio/problems/:problemId", async ({ params }) => {
        await delay();
        const problem = findAccessibleStudioProblem(String(params.problemId));
        return problem
            ? HttpResponse.json(toStudioProblemDetail(problem))
            : apiError(404, "PROBLEM_NOT_FOUND", "Không tìm thấy bài tập");
    }),

    http.delete("*/api/studio/problems/:problemId", async ({ params }) => {
        await delay();
        const problem = findAccessibleStudioProblem(String(params.problemId));
        if (!problem) {
            return apiError(404, "PROBLEM_NOT_FOUND", "Không tìm thấy bài tập");
        }
        if (!isEditable(problem) && readMockSession()?.role !== "ADMIN") {
            return apiError(422, "PROBLEM_LOCKED", "Chỉ xoá được bài nháp hoặc bài cần sửa");
        }
        writeStudioProblems(
            readStudioProblems().map((stored) =>
                stored.id === problem.id
                    ? { ...stored, deletedAt: new Date().toISOString() }
                    : stored,
            ),
        );
        return new HttpResponse(null, { status: 204 });
    }),

    http.put("*/api/studio/problems/:problemId/dataset", async ({ params, request }) => {
        await delay();
        const problem = findAccessibleStudioProblem(String(params.problemId));
        if (!problem) {
            return apiError(404, "PROBLEM_NOT_FOUND", "Không tìm thấy bài tập");
        }
        if (!isEditable(problem)) {
            return apiError(422, "PROBLEM_LOCKED", "Bài đang chờ duyệt hoặc đã xuất bản");
        }
        const { datasetId } = (await request.json()) as { datasetId?: string };
        if (!datasetId) {
            return validationFailed({ field: "datasetId", message: "Chưa chọn database" });
        }
        const user = readMockSession();
        const dataset = readStudioDatasets().find(
            (candidate) =>
                candidate.id === datasetId &&
                !candidate.deletedAt &&
                (candidate.ownerId === user?.id || user?.role === "ADMIN"),
        );
        if (!dataset) {
            return apiError(404, "DATASET_NOT_FOUND", "Không tìm thấy database");
        }
        const notReadyReason = datasetNotReadyReason(dataset);
        if (notReadyReason) {
            return apiError(422, "DATASET_NOT_READY", notReadyReason);
        }
        const answer =
            problem.answer && problem.datasetId !== datasetId
                ? { ...problem.answer, testcaseVariantIds: [] }
                : problem.answer;
        const updated = { ...problem, datasetId, answer, updatedAt: new Date().toISOString() };
        saveProblem(updated);
        return HttpResponse.json(toStudioProblemDetail(updated));
    }),

    http.get("*/api/studio/problems/:problemId/answer", async ({ params }) => {
        await delay();
        const problem = findAccessibleStudioProblem(String(params.problemId));
        return problem
            ? HttpResponse.json(answerOf(problem))
            : apiError(404, "PROBLEM_NOT_FOUND", "Không tìm thấy bài tập");
    }),

    http.put("*/api/studio/problems/:problemId/answer", async ({ params, request }) => {
        await delay();
        const problem = findAccessibleStudioProblem(String(params.problemId));
        if (!problem) {
            return apiError(404, "PROBLEM_NOT_FOUND", "Không tìm thấy bài tập");
        }
        if (!isEditable(problem)) {
            return apiError(422, "PROBLEM_LOCKED", "Bài đang chờ duyệt hoặc đã xuất bản");
        }
        if (!problem.datasetId) {
            return apiError(422, "PROBLEM_DATASET_MISSING", "Bài chưa chọn database");
        }
        const answer = (await request.json()) as SaveProblemAnswerRequest;
        if (!answer.solutions.some((solution) => solution.dialect === "POSTGRESQL")) {
            return validationFailed({ field: "solutions", message: "Cần có đáp án PostgreSQL" });
        }
        const updated = { ...problem, answer, updatedAt: new Date().toISOString() };
        saveProblem(updated);
        return HttpResponse.json(answerOf(updated));
    }),

    http.patch("*/api/studio/problems/:problemId", async ({ params, request }) => {
        await delay();
        const problem = findAccessibleStudioProblem(String(params.problemId));
        if (!problem) {
            return apiError(404, "PROBLEM_NOT_FOUND", "Không tìm thấy bài tập");
        }
        if (!isEditable(problem)) {
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
        return HttpResponse.json(toStudioProblemDetail(updated));
    }),

    http.get("*/api/studio/problems", async ({ request }) => {
        await delay();
        const user = readMockSession();
        if (!user) {
            return apiError(401, "UNAUTHENTICATED", "Bạn chưa đăng nhập");
        }
        const query = new URL(request.url).searchParams;
        const difficulty = query.get("difficulty");
        const sort = query.get("sort") ?? "updatedAt,desc";
        const keyword = folded(query.get("q")?.trim() ?? "");
        const page = Number(query.get("page") ?? 1);
        const size = Number(query.get("size") ?? DEFAULT_PAGE_SIZE);
        const matches = sortedProblems(
            ownProblems(user.id).filter(
                (problem) =>
                    (difficulty === null || problem.difficulty === difficulty) &&
                    folded(problem.title).includes(keyword),
            ),
            sort,
        );
        const response: Page<StudioProblemSummary> = {
            items: matches.slice((page - 1) * size, page * size).map(toSummary),
            page,
            size,
            totalItems: matches.length,
            totalPages: Math.max(1, Math.ceil(matches.length / size)),
        };
        return HttpResponse.json(response);
    }),
];
