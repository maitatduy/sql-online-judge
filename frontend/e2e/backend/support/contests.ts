import { execFileSync } from "node:child_process";

import { type APIRequestContext, expect } from "@playwright/test";

import type { Learner } from "./learner";

const API_URL = process.env.E2E_API_URL ?? "http://localhost:8080";
const FRONTEND_ORIGIN = "http://localhost:3000";
const MYSQL_CONTAINER = process.env.E2E_MYSQL_CONTAINER ?? "sqljudge-dev-mysql-1";
const E2E_SLUG = /^e2e-[a-z0-9-]+$/;
const PROBLEM_SLUG = /^[a-z0-9-]+$/;

export async function accessTokenOf(request: APIRequestContext, user: Learner): Promise<string> {
    const response = await request.post(`${API_URL}/api/auth/login`, {
        data: user,
        headers: { Origin: FRONTEND_ORIGIN },
    });
    expect(response.status()).toBe(200);
    return ((await response.json()) as { accessToken: string }).accessToken;
}

export async function createContest(
    request: APIRequestContext,
    adminToken: string,
    slug: string,
): Promise<void> {
    const response = await request.post(`${API_URL}/api/admin/contests`, {
        headers: { Authorization: `Bearer ${adminToken}` },
        data: {
            slug,
            title: `Contest ${slug}`,
            description: "Contest thử nghiệm tự động.",
            startAt: new Date(Date.now() + 60 * 60 * 1000).toISOString(),
            durationMinutes: 90,
            freezeMinutes: 30,
            penaltyMinutes: 20,
            isRated: false,
        },
    });
    expect(response.status()).toBe(201);
}

export function attachProblemAndStart(contestSlug: string, problemSlug: string): void {
    if (!E2E_SLUG.test(contestSlug) || !PROBLEM_SLUG.test(problemSlug)) {
        throw new Error(`Chỉ sửa contest thử nghiệm: ${contestSlug}`);
    }
    runSql(
        [
            "INSERT INTO sqljudge_contest.contest_problems",
            "(id, contest_id, problem_id, position, slug, title, difficulty, created_at, updated_at)",
            "SELECT UUID_TO_BIN(UUID()), c.id, p.id, 0, p.slug, p.title, p.difficulty, NOW(6), NOW(6)",
            "FROM sqljudge_contest.contests c, sqljudge_problem.problems p",
            `WHERE c.slug = '${contestSlug}' AND p.slug = '${problemSlug}';`,
            "UPDATE sqljudge_contest.contests",
            "SET start_at = DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 5 MINUTE),",
            "end_at = DATE_ADD(UTC_TIMESTAMP(6), INTERVAL 85 MINUTE)",
            `WHERE slug = '${contestSlug}';`,
        ].join(" "),
    );
}

function runSql(statement: string): void {
    execFileSync("docker", [
        "exec",
        MYSQL_CONTAINER,
        "sh",
        "-c",
        `mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "${statement}" 2>/dev/null`,
    ]);
}
