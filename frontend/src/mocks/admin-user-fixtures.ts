import type { AccountStatus, AdminUserSummary } from "@/features/admin/users/types";
import type { Role } from "@/lib/current-user";

import { ADMIN_SESSION_USER, DEMO_SESSION_USER } from "./session";

const MINUTE_MS = 60_000;
const HOUR_MINUTES = 60;
const DAY_MINUTES = 24 * HOUR_MINUTES;
const DAY_MS = DAY_MINUTES * MINUTE_MS;
const GENERATED_LEARNER_COUNT = 50;
const GENERATED_JOIN_START_MS = Date.parse("2025-04-10T03:00:00Z");
const GENERATED_JOIN_STEP_DAYS = 10;
const SESSION_USER_IDS: Record<string, string> = {
    [ADMIN_SESSION_USER.username]: ADMIN_SESSION_USER.id,
    [DEMO_SESSION_USER.username]: DEMO_SESSION_USER.id,
};

interface UserSeed {
    username: string;
    role: Role;
    rating: number;
    acceptedCount: number;
    joinedOn: string;
    idleMinutes: number | null;
    status?: AccountStatus;
    bannedForDays?: number;
    isEmailVerified?: boolean;
    emailDomain?: string;
}

const STAFF_EMAIL_DOMAIN = "sqljudge.dev";

const NAMED_SEEDS: UserSeed[] = [
    {
        username: "quang.le",
        role: "SETTER",
        rating: 2184,
        acceptedCount: 412,
        joinedOn: "2025-03-14",
        idleMinutes: 5,
    },
    {
        username: "trang.hoang",
        role: "MODERATOR",
        rating: 2097,
        acceptedCount: 388,
        joinedOn: "2025-04-02",
        idleMinutes: HOUR_MINUTES,
    },
    {
        username: "datahust",
        role: "SETTER",
        rating: 0,
        acceptedCount: 12,
        joinedOn: "2026-09-20",
        idleMinutes: 2 * HOUR_MINUTES,
    },
    {
        username: "minh.anh",
        role: "LEARNER",
        rating: 1487,
        acceptedCount: 63,
        joinedOn: "2026-01-11",
        idleMinutes: null,
    },
    {
        username: "viet.luu",
        role: "LEARNER",
        rating: 1212,
        acceptedCount: 41,
        joinedOn: "2026-02-03",
        idleMinutes: 12,
    },
    {
        username: "spam.bot88",
        role: "LEARNER",
        rating: 0,
        acceptedCount: 0,
        joinedOn: "2026-10-10",
        idleMinutes: DAY_MINUTES,
        status: "BANNED",
    },
    {
        username: "hoa.kieu",
        role: "LEARNER",
        rating: 1398,
        acceptedCount: 77,
        joinedOn: "2026-05-22",
        idleMinutes: 3 * HOUR_MINUTES,
    },
    {
        username: "admin.huy",
        role: "ADMIN",
        rating: 0,
        acceptedCount: 0,
        joinedOn: "2025-01-01",
        idleMinutes: null,
        emailDomain: STAFF_EMAIL_DOMAIN,
    },
    {
        username: "tam.nguyen",
        role: "LEARNER",
        rating: 0,
        acceptedCount: 0,
        joinedOn: "2026-10-11",
        idleMinutes: 10,
        isEmailVerified: false,
    },
    {
        username: "mod.thu",
        role: "MODERATOR",
        rating: 1798,
        acceptedCount: 264,
        joinedOn: "2025-06-18",
        idleMinutes: 40,
        emailDomain: STAFF_EMAIL_DOMAIN,
    },
    {
        username: "copy.paste",
        role: "LEARNER",
        rating: 1102,
        acceptedCount: 95,
        joinedOn: "2026-08-08",
        idleMinutes: 5 * DAY_MINUTES,
        status: "BANNED",
        bannedForDays: 24,
    },
    {
        username: "an.bui",
        role: "SETTER",
        rating: 1820,
        acceptedCount: 199,
        joinedOn: "2025-07-30",
        idleMinutes: DAY_MINUTES,
    },
];

function generatedSeed(index: number): UserSeed {
    const joinedAtMs = GENERATED_JOIN_START_MS + index * GENERATED_JOIN_STEP_DAYS * DAY_MS;
    return {
        username: `learner.${String(index + 1).padStart(2, "0")}`,
        role: "LEARNER",
        rating: 900 + ((index * 53) % 700),
        acceptedCount: (index * 7) % 120,
        joinedOn: new Date(joinedAtMs).toISOString().slice(0, 10),
        idleMinutes: (index + 1) * DAY_MINUTES,
    };
}

function userFrom(seed: UserSeed, index: number, now: Date): AdminUserSummary {
    const { bannedForDays, idleMinutes } = seed;
    return {
        id:
            SESSION_USER_IDS[seed.username] ??
            `0192f3a1-0000-7000-8000-${index.toString(16).padStart(12, "0")}`,
        username: seed.username,
        email: `${seed.username}@${seed.emailDomain ?? "example.com"}`,
        role: seed.role,
        rating: seed.rating,
        acceptedCount: seed.acceptedCount,
        joinedAt: `${seed.joinedOn}T03:00:00Z`,
        lastActiveAt: new Date(now.getTime() - (idleMinutes ?? 0) * MINUTE_MS).toISOString(),
        isOnline: idleMinutes === null,
        status: seed.status ?? "ACTIVE",
        bannedUntil: bannedForDays
            ? new Date(now.getTime() + bannedForDays * DAY_MS).toISOString()
            : null,
        isEmailVerified: seed.isEmailVerified ?? true,
        avatarUrl: null,
        deletedAt: null,
    };
}

export function buildAdminUsers(now: Date): AdminUserSummary[] {
    const generatedSeeds = Array.from({ length: GENERATED_LEARNER_COUNT }, (_, index) =>
        generatedSeed(index),
    );
    return [...NAMED_SEEDS, ...generatedSeeds].map((seed, index) => userFrom(seed, index, now));
}
