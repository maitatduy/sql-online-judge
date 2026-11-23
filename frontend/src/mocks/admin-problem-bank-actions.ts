import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type { QuickEditValues } from "@/features/admin/problem-bank/schemas";

import { validationFailed } from "./admin-user-errors";
import { apiError } from "./api-error";
import { ADMIN_SESSION_USER, DEMO_SESSION_USER, SETTER_SESSION_USER } from "./session";
import { readStudioProblems, type StoredProblem, writeStudioProblems } from "./studio-problems";

const SLUGS_IN_UNFINISHED_CONTEST = new Set(["booking-gaps"]);
const NON_SETTER_USERNAMES = new Set([ADMIN_SESSION_USER.username, DEMO_SESSION_USER.username]);

type Change = (problem: StoredProblem) => StoredProblem | Response;

function publishedProblem(problemId: string): StoredProblem | undefined {
    return readStudioProblems().find(
        (problem) =>
            problem.id === problemId && !problem.deletedAt && problem.status === "PUBLISHED",
    );
}

function applyChange(problemId: string, change: Change): Response {
    const problem = publishedProblem(problemId);
    if (!problem) {
        return apiError(404, "PROBLEM_NOT_FOUND", "Không tìm thấy bài tập");
    }
    const changed = change(problem);
    if (changed instanceof Response) {
        return changed;
    }
    writeStudioProblems(
        readStudioProblems().map((candidate) => (candidate.id === problemId ? changed : candidate)),
    );
    return new HttpResponse(null, { status: 204 });
}

function inContest(problem: StoredProblem): Response | null {
    return SLUGS_IN_UNFINISHED_CONTEST.has(problem.slug)
        ? apiError(409, "PROBLEM_IN_ACTIVE_CONTEST", "Bài đang thuộc một contest chưa kết thúc")
        : null;
}

function unchanged(message: string): Response {
    return apiError(422, "PROBLEM_VISIBILITY_UNCHANGED", message);
}

export const adminProblemBankActionHandlers: RequestHandler[] = [
    http.patch("*/api/admin/problems/:problemId", async ({ params, request }) => {
        await delay();
        const values = (await request.json()) as QuickEditValues;
        if (!values.title.trim()) {
            return validationFailed({ field: "title", message: "Không được để trống" });
        }
        return applyChange(String(params.problemId), (problem) => ({
            ...problem,
            title: values.title.trim(),
            difficulty: values.difficulty as StoredProblem["difficulty"],
            tagSlugs: values.tagSlugs,
        }));
    }),

    http.post("*/api/admin/problems/:problemId/hide", async ({ params }) => {
        await delay();
        return applyChange(String(params.problemId), (problem) => {
            const visibility = problem.visibility ?? "PUBLIC";
            if (visibility === "HIDDEN") {
                return unchanged("Bài đang ẩn");
            }
            return (
                inContest(problem) ?? {
                    ...problem,
                    visibility: "HIDDEN",
                    hiddenFromVisibility: visibility,
                }
            );
        });
    }),

    http.post("*/api/admin/problems/:problemId/restore", async ({ params }) => {
        await delay();
        return applyChange(String(params.problemId), (problem) =>
            problem.visibility === "HIDDEN"
                ? {
                      ...problem,
                      visibility: problem.hiddenFromVisibility ?? "PUBLIC",
                      hiddenFromVisibility: undefined,
                  }
                : unchanged("Bài không bị ẩn"),
        );
    }),

    http.post("*/api/admin/problems/:problemId/transfer-ownership", async ({ params, request }) => {
        await delay();
        const { username } = (await request.json()) as { username: string };
        return applyChange(String(params.problemId), (problem) => {
            if (NON_SETTER_USERNAMES.has(username)) {
                return apiError(
                    422,
                    "NEW_OWNER_NOT_SETTER",
                    "Người nhận phải là người ra đề đang hoạt động",
                );
            }
            if (username !== SETTER_SESSION_USER.username) {
                return apiError(404, "USER_NOT_FOUND", "Không tìm thấy người dùng");
            }
            return { ...problem, authorId: SETTER_SESSION_USER.id };
        });
    }),

    http.delete("*/api/admin/problems/:problemId", async ({ params }) => {
        await delay();
        return applyChange(
            String(params.problemId),
            (problem) => inContest(problem) ?? { ...problem, deletedAt: new Date().toISOString() },
        );
    }),
];
