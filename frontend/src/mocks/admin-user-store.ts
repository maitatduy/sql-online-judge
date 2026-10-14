import type {
    AdminUserDetail,
    AdminUserSummary,
    UserViolation,
} from "@/features/admin/users/types";

import { buildAdminUserDetail } from "./admin-user-details";
import { buildAdminUsers } from "./admin-user-fixtures";

const CHANGES_STORAGE_KEY = "sqljudge:mock:admin-user-changes";

type UserFields = Partial<Pick<AdminUserSummary, "role" | "status" | "bannedUntil">>;
type NewViolation = Omit<UserViolation, "id">;

interface UserChange {
    fields?: UserFields;
    violations?: NewViolation[];
}

function readChanges(): Record<string, UserChange> {
    const storedChanges = sessionStorage.getItem(CHANGES_STORAGE_KEY);
    return storedChanges ? (JSON.parse(storedChanges) as Record<string, UserChange>) : {};
}

function writeChange(username: string, change: UserChange): void {
    sessionStorage.setItem(
        CHANGES_STORAGE_KEY,
        JSON.stringify({ ...readChanges(), [username]: change }),
    );
}

export function readAdminUsers(now: Date): AdminUserSummary[] {
    const changes = readChanges();
    return buildAdminUsers(now).map((user) => ({ ...user, ...changes[user.username]?.fields }));
}

export function findAdminUser(username: string): AdminUserSummary | undefined {
    return readAdminUsers(new Date()).find((user) => user.username === username);
}

export function readAdminUserDetail(username: string): AdminUserDetail | undefined {
    const user = findAdminUser(username);
    return user && buildAdminUserDetail(user, readChanges()[username]?.violations);
}

export function changeAdminUser(username: string, fields: UserFields): void {
    const change = readChanges()[username];
    writeChange(username, { ...change, fields: { ...change?.fields, ...fields } });
}

export function addUserViolation(username: string, violation: NewViolation): void {
    const change = readChanges()[username];
    writeChange(username, { ...change, violations: [...(change?.violations ?? []), violation] });
}
