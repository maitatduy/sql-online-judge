import { ROUTES } from "@/lib/routes";

import { AUDIT_ACTIONS } from "./action-labels";
import type { AuditAction } from "./types";

export const AUDIT_PAGE_SIZE = 20;

export const AUDIT_PERIODS = ["today", "7d", "30d"] as const;

export type AuditPeriod = (typeof AUDIT_PERIODS)[number];

export const AUDIT_PERIOD_LABELS: Record<AuditPeriod, string> = {
    today: "Hôm nay",
    "7d": "7 ngày qua",
    "30d": "30 ngày qua",
};

export interface AuditFilters {
    user: string | null;
    actor: string | null;
    action: AuditAction | null;
    period: AuditPeriod | null;
    page: number;
}

export interface AuditQuery {
    user?: string;
    actor?: string;
    action?: AuditAction;
    from?: string;
}

const FIRST_PAGE = 1;
const DAY_MS = 24 * 60 * 60_000;
const VIETNAM_OFFSET_MS = 7 * 60 * 60_000;
const PERIOD_DAYS: Record<Exclude<AuditPeriod, "today">, number> = { "7d": 7, "30d": 30 };

const DEFAULT_PERIOD: AuditPeriod = "7d";
const ALL_TIME_PARAM = "all";

export const NO_AUDIT_FILTERS: AuditFilters = {
    user: null,
    actor: null,
    action: null,
    period: DEFAULT_PERIOD,
    page: FIRST_PAGE,
};

function defaultPeriodOf(user: string | null): AuditPeriod | null {
    return user ? null : DEFAULT_PERIOD;
}

function readPeriod(value: string | null, user: string | null): AuditPeriod | null {
    if (value === ALL_TIME_PARAM) {
        return null;
    }
    return knownValue(AUDIT_PERIODS, value) ?? defaultPeriodOf(user);
}

function knownValue<T extends string>(knownValues: readonly T[], value: string | null): T | null {
    return knownValues.find((knownValue) => knownValue === value) ?? null;
}

function readText(value: string | null): string | null {
    return value?.trim() || null;
}

function readPage(value: string | null): number {
    const page = Number(value);
    return Number.isInteger(page) && page >= FIRST_PAGE ? page : FIRST_PAGE;
}

function startOfVietnamDay(now: Date): Date {
    const vietnamMs = now.getTime() + VIETNAM_OFFSET_MS;
    return new Date(vietnamMs - (vietnamMs % DAY_MS) - VIETNAM_OFFSET_MS);
}

function fromOf(period: AuditPeriod, now: Date): Date {
    return period === "today"
        ? startOfVietnamDay(now)
        : new Date(now.getTime() - PERIOD_DAYS[period] * DAY_MS);
}

export function readAuditFilters(searchParams: Pick<URLSearchParams, "get">): AuditFilters {
    const user = readText(searchParams.get("user"));
    return {
        user,
        actor: readText(searchParams.get("actor")),
        action: knownValue(AUDIT_ACTIONS, searchParams.get("action")),
        period: readPeriod(searchParams.get("period"), user),
        page: readPage(searchParams.get("page")),
    };
}

export function auditHref({ user, actor, action, period, page }: AuditFilters): string {
    const query = new URLSearchParams();
    if (user) {
        query.set("user", user);
    }
    if (actor) {
        query.set("actor", actor);
    }
    if (action) {
        query.set("action", action);
    }
    if (period !== defaultPeriodOf(user)) {
        query.set("period", period ?? ALL_TIME_PARAM);
    }
    if (page > FIRST_PAGE) {
        query.set("page", String(page));
    }
    const queryString = query.toString();
    return queryString ? `${ROUTES.adminAuditLog}?${queryString}` : ROUTES.adminAuditLog;
}

export function auditQueryOf({ user, actor, action, period }: AuditFilters, now: Date): AuditQuery {
    return {
        ...(user ? { user } : {}),
        ...(actor ? { actor } : {}),
        ...(action ? { action } : {}),
        ...(period ? { from: fromOf(period, now).toISOString() } : {}),
    };
}

export function hasActiveAuditFilter({ user, actor, action, period }: AuditFilters): boolean {
    return user !== null || actor !== null || action !== null || period !== defaultPeriodOf(user);
}
