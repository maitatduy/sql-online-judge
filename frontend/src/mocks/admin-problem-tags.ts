import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type { AdminTag, BulkUpdateRequest } from "@/features/admin/problem-bank/types";

import { readAdminTags, tagSlugsOf } from "./admin-problem-bank";
import { readStudioProblems, type StoredProblem, writeStudioProblems } from "./studio-problems";

const SLUGS_IN_UNFINISHED_CONTEST = new Set(["booking-gaps"]);

function withCounts(): AdminTag[] {
    const liveProblems = readStudioProblems().filter((problem) => !problem.deletedAt);
    return readAdminTags()
        .map((tag) => ({
            ...tag,
            problemCount: liveProblems.filter((problem) => tagSlugsOf(problem).includes(tag.slug))
                .length,
        }))
        .sort((first, second) => first.name.localeCompare(second.name));
}

function bulkChange(problem: StoredProblem, request: BulkUpdateRequest): StoredProblem | string {
    if (request.hide) {
        if (SLUGS_IN_UNFINISHED_CONTEST.has(problem.slug)) {
            return "PROBLEM_IN_ACTIVE_CONTEST";
        }
        if (problem.visibility === "HIDDEN") {
            return "PROBLEM_VISIBILITY_UNCHANGED";
        }
    }
    const tagSlugs = request.addTagSlugs
        ? [...new Set([...tagSlugsOf(problem), ...request.addTagSlugs])]
        : problem.tagSlugs;
    return {
        ...problem,
        tagSlugs,
        difficulty: (request.difficulty as StoredProblem["difficulty"]) ?? problem.difficulty,
        visibility: request.hide ? "HIDDEN" : problem.visibility,
        hiddenFromVisibility: request.hide
            ? (problem.visibility ?? "PUBLIC")
            : problem.hiddenFromVisibility,
    };
}

export const adminProblemTagHandlers: RequestHandler[] = [
    http.get("*/api/admin/tags", async () => {
        await delay();
        return HttpResponse.json(withCounts());
    }),

    http.post("*/api/admin/problems/bulk-update", async ({ request }) => {
        await delay();
        const body = (await request.json()) as BulkUpdateRequest;
        const problems = readStudioProblems();
        const succeeded: string[] = [];
        const failed: { problemId: string; code: string; message: string }[] = [];
        const updated = problems.map((problem) => {
            if (!body.problemIds.includes(problem.id)) {
                return problem;
            }
            const changed = bulkChange(problem, body);
            if (typeof changed === "string") {
                failed.push({ problemId: problem.id, code: changed, message: changed });
                return problem;
            }
            succeeded.push(problem.id);
            return changed;
        });
        writeStudioProblems(updated);
        return HttpResponse.json({ succeeded, failed });
    }),
];
