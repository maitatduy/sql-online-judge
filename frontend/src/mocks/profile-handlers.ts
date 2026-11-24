import { delay, http, HttpResponse, type RequestHandler } from "msw";

import { buildActiveYear, EMPTY_ACTIVITY } from "./activity-fixtures";
import { adminUserProfile, adminUserProfileById } from "./admin-user-profiles";
import { apiError } from "./api-error";
import {
    BROKEN_PROFILE_USER_ID,
    BROKEN_PROFILE_USERNAME,
    MINH_ANH_PROFILE,
    MOCK_TRACE_ID,
    PROFILES,
    RATING_HISTORY_BY_USERNAME,
} from "./profile-fixtures";
import { buildRecentSubmissions } from "./submission-fixtures";

const DEFAULT_PAGE_SIZE = 20;
const MAX_PAGE_SIZE = 100;

function findPublicProfile(username: string) {
    return (
        PROFILES.find((mock) => mock.profile.username === username) ?? adminUserProfile(username)
    );
}

function findByUserId(userId: string) {
    return PROFILES.find((mock) => mock.profile.userId === userId) ?? adminUserProfileById(userId);
}

function brokenUserResponse(userId: string) {
    return userId === BROKEN_PROFILE_USER_ID
        ? apiError(500, "INTERNAL_ERROR", "Đã có lỗi xảy ra, vui lòng thử lại sau", {
              traceId: MOCK_TRACE_ID,
          })
        : null;
}

function isMinhAnh(userId: string): boolean {
    return userId === MINH_ANH_PROFILE.profile.userId;
}

function unavailableProfileResponse(username: string) {
    if (username === BROKEN_PROFILE_USERNAME) {
        return apiError(500, "INTERNAL_ERROR", "Đã có lỗi xảy ra, vui lòng thử lại sau", {
            traceId: MOCK_TRACE_ID,
        });
    }
    if (!findPublicProfile(username)) {
        return apiError(404, "USER_NOT_FOUND", "Không tìm thấy người dùng");
    }
    return null;
}

export const profileHandlers: RequestHandler[] = [
    http.get("*/api/leaderboard/:userId", async ({ params }) => {
        await delay();
        const userId = String(params.userId);
        return HttpResponse.json(
            findByUserId(userId)?.rating ?? {
                userId,
                rating: null,
                rank: null,
                contestsPlayed: 0,
            },
        );
    }),

    http.get("*/api/solved-summaries/:userId", async ({ params }) => {
        await delay();
        const userId = String(params.userId);
        return HttpResponse.json(
            findByUserId(userId)?.solved ?? {
                userId,
                solvedCount: 0,
                byDifficulty: {
                    EASY: { solved: 0, total: 0 },
                    MEDIUM: { solved: 0, total: 0 },
                    HARD: { solved: 0, total: 0 },
                },
            },
        );
    }),

    http.get("*/api/users/:username", async ({ params }) => {
        await delay();
        const username = String(params.username);
        return (
            unavailableProfileResponse(username) ??
            HttpResponse.json(findPublicProfile(username)?.profile)
        );
    }),

    http.get("*/api/leaderboard/:userId/history", async ({ params }) => {
        await delay();
        const broken = brokenUserResponse(String(params.userId));
        if (broken) {
            return broken;
        }
        const username = findByUserId(String(params.userId))?.profile.username;
        return HttpResponse.json({
            items: username ? (RATING_HISTORY_BY_USERNAME[username] ?? []) : [],
        });
    }),

    http.get("*/api/submissions/users/:userId/activity", async ({ params }) => {
        await delay();
        const broken = brokenUserResponse(String(params.userId));
        if (broken) {
            return broken;
        }
        return HttpResponse.json(
            isMinhAnh(String(params.userId)) ? buildActiveYear(new Date()) : EMPTY_ACTIVITY,
        );
    }),

    http.get("*/api/submissions/users/:userId", async ({ params, request }) => {
        await delay();
        const broken = brokenUserResponse(String(params.userId));
        if (broken) {
            return broken;
        }
        const query = new URL(request.url).searchParams;
        const startIndex = Number(query.get("cursor") ?? 0);
        const pageSize = Math.min(Number(query.get("size") ?? DEFAULT_PAGE_SIZE), MAX_PAGE_SIZE);
        const submissions = isMinhAnh(String(params.userId))
            ? buildRecentSubmissions(new Date())
            : [];
        const endIndex = startIndex + pageSize;
        return HttpResponse.json({
            items: submissions.slice(startIndex, endIndex),
            nextCursor: endIndex < submissions.length ? String(endIndex) : null,
        });
    }),
];
