import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type { AuditAction, AuditLogEntry, AuditLogResponse } from "@/features/admin/audit/types";
import type { Role } from "@/lib/current-user";

const DEFAULT_PAGE_SIZE = 20;
const NEWEST_AT_MS = Date.parse("2026-10-13T07:05:12Z");
const STEP_MS = 3 * 60 * 60_000;

type SeedEntry = [
    actor: string,
    actorRole: Role,
    action: AuditAction,
    target: string | null,
    reason: string | null,
    previousValue?: string | null,
    newValue?: string | null,
];

const SEED: SeedEntry[] = [
    [
        "admin.huy",
        "ADMIN",
        "UPDATE_ROLE",
        "datahust",
        "CLB được duyệt làm người ra đề",
        "LEARNER",
        "SETTER",
    ],
    ["admin.huy", "ADMIN", "BAN", "spam.bot88", "Tài khoản spam"],
    [
        "admin.huy",
        "ADMIN",
        "UPDATE_RESTRICTIONS",
        "viet.luu",
        "Nghi đạo code trong contest",
        "AI 20/ngày",
        "AI 5/ngày",
    ],
    ["admin.huy", "ADMIN", "REVOKE_ALL_SESSIONS", "viet.luu", null],
    ["admin.huy", "ADMIN", "EXPORT_USER_EMAILS", null, "Đối soát danh sách CLB"],
    ["admin.huy", "ADMIN", "BAN", "copy.paste", "Nộp bài trùng code người khác"],
    ["admin.huy", "ADMIN", "SEND_PASSWORD_RESET", "hoa.kieu", null],
    ["admin.huy", "ADMIN", "UPDATE_DISPLAY_NAME", "minh.anh", null, "minh", "Nguyễn Minh Anh"],
    ["admin.huy", "ADMIN", "SEND_VERIFICATION", "tam.nguyen", null],
    ["admin.huy", "ADMIN", "DELETE", "test.account", "Tạo nhầm"],
    ["admin.huy", "ADMIN", "RESTORE", "test.account", null],
    ["admin.huy", "ADMIN", "UPDATE_AVATAR", "quang.le", null],
    ["admin.huy", "ADMIN", "UNBAN", "viet.luu", "Đã xác minh không vi phạm"],
    ["admin.huy", "ADMIN", "BAN", "viet.luu", "Nghi đạo code trong contest"],
    [
        "admin.huy",
        "ADMIN",
        "UPDATE_ROLE",
        "mod.thu",
        "Hỗ trợ kiểm duyệt cộng đồng",
        "LEARNER",
        "MODERATOR",
    ],
    ["admin.huy", "ADMIN", "REMOVE_AVATAR", "spam.bot88", null],
    ["admin.huy", "ADMIN", "REVOKE_SESSION", "trang.hoang", null],
    [
        "admin.huy",
        "ADMIN",
        "UPDATE_ROLE",
        "an.bui",
        "Ra đề cho Window Function Cup",
        "LEARNER",
        "SETTER",
    ],
    ["admin.huy", "ADMIN", "SEND_VERIFICATION", "datahust", null],
    ["admin.huy", "ADMIN", "UPDATE_DISPLAY_NAME", "datahust", null, null, "CLB Data HUST"],
    ["admin.huy", "ADMIN", "BAN", "acc.phu.01", "Dùng nhiều tài khoản trong contest"],
    ["admin.huy", "ADMIN", "BAN", "acc.phu.02", "Dùng nhiều tài khoản trong contest"],
    ["admin.huy", "ADMIN", "SEND_PASSWORD_RESET", "viet.luu", null],
    [
        "admin.huy",
        "ADMIN",
        "UPDATE_ROLE",
        "trang.hoang",
        "Ra đề cho SQL Weekly",
        "LEARNER",
        "SETTER",
    ],
];

const AUDIT_LOG: AuditLogEntry[] = SEED.map(
    (
        [actorUsername, actorRole, action, targetUsername, reason, previousValue, newValue],
        index,
    ) => ({
        id: `audit-${index + 1}`,
        occurredAt: new Date(NEWEST_AT_MS - index * STEP_MS).toISOString(),
        actorUsername,
        actorRole,
        targetUsername,
        action,
        reason,
        previousValue: previousValue ?? null,
        newValue: newValue ?? null,
        expiresAt: null,
        details: action === "EXPORT_USER_EMAILS" ? "12 tài khoản" : null,
    }),
);

function matches(entry: AuditLogEntry, query: URLSearchParams): boolean {
    const user = query.get("user");
    const actor = query.get("actor");
    const action = query.get("action");
    const from = query.get("from");
    return (
        (user === null || entry.targetUsername === user) &&
        (actor === null || entry.actorUsername === actor) &&
        (action === null || entry.action === action) &&
        (from === null || entry.occurredAt >= new Date(from).toISOString())
    );
}

function integerParam(value: string | null, fallback: number): number {
    const parsed = Number(value);
    return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback;
}

export const adminAuditLogHandlers: RequestHandler[] = [
    http.get("*/api/admin/audit-log", async ({ request }) => {
        await delay();
        const query = new URL(request.url).searchParams;
        const page = integerParam(query.get("page"), 1);
        const size = integerParam(query.get("size"), DEFAULT_PAGE_SIZE);
        const entries = AUDIT_LOG.filter((entry) => matches(entry, query));
        const firstIndex = (page - 1) * size;
        const response: AuditLogResponse = {
            items: entries.slice(firstIndex, firstIndex + size),
            page,
            size,
            totalItems: entries.length,
            totalPages: Math.ceil(entries.length / size),
        };
        return HttpResponse.json(response);
    }),
];
