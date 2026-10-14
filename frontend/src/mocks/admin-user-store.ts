import type {
    AdminUserDetail,
    AdminUserSummary,
    UserViolation,
} from "@/features/admin/users/types";

import { buildAdminUserDetail } from "./admin-user-details";
import { buildAdminUsers } from "./admin-user-fixtures";

const CHANGES_STORAGE_KEY = "sqljudge:mock:admin-user-changes";
const CREATED_STORAGE_KEY = "sqljudge:mock:admin-created-users";

type UserFields = Partial<
    Pick<AdminUserSummary, "role" | "status" | "bannedUntil" | "avatarUrl" | "deletedAt">
>;
type NewViolation = Omit<UserViolation, "id">;

type UserProfile = Pick<AdminUserDetail, "displayName">;

interface UserChange {
    fields?: UserFields;
    profile?: UserProfile;
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

function readCreatedUsers(): AdminUserSummary[] {
    const storedUsers = sessionStorage.getItem(CREATED_STORAGE_KEY);
    return storedUsers ? (JSON.parse(storedUsers) as AdminUserSummary[]) : [];
}

function readAllAdminUsers(now: Date): AdminUserSummary[] {
    return [...readCreatedUsers(), ...buildAdminUsers(now)];
}

export function isTakenByAnyUser(isTaken: (user: AdminUserSummary) => boolean): boolean {
    return readAllAdminUsers(new Date()).some(isTaken);
}

export function createAdminUser(user: AdminUserSummary, profile: UserProfile): void {
    sessionStorage.setItem(CREATED_STORAGE_KEY, JSON.stringify([user, ...readCreatedUsers()]));
    changeAdminUserProfile(user.username, profile);
}

function readChangedAdminUsers(now: Date): AdminUserSummary[] {
    const changes = readChanges();
    return readAllAdminUsers(now).map((user) => ({ ...user, ...changes[user.username]?.fields }));
}

export function readAdminUsers(now: Date): AdminUserSummary[] {
    return readChangedAdminUsers(now).filter((user) => user.deletedAt === null);
}

export function readDeletedAdminUsers(now: Date): AdminUserSummary[] {
    return readChangedAdminUsers(now).filter((user) => user.deletedAt !== null);
}

export function findAdminUser(username: string): AdminUserSummary | undefined {
    return readAdminUsers(new Date()).find((user) => user.username === username);
}

export function findAnyAdminUser(username: string): AdminUserSummary | undefined {
    return readChangedAdminUsers(new Date()).find((user) => user.username === username);
}

export function readAdminUserDetail(username: string): AdminUserDetail | undefined {
    const user = findAnyAdminUser(username);
    const change = readChanges()[username];
    return user && { ...buildAdminUserDetail(user, change?.violations), ...change?.profile };
}

export function changeAdminUser(username: string, fields: UserFields): void {
    const change = readChanges()[username];
    writeChange(username, { ...change, fields: { ...change?.fields, ...fields } });
}

export function changeAdminUserProfile(username: string, profile: UserProfile): void {
    writeChange(username, { ...readChanges()[username], profile });
}

export function deleteAdminUser(username: string): void {
    changeAdminUser(username, { deletedAt: new Date().toISOString() });
}

export function restoreAdminUser(username: string): void {
    changeAdminUser(username, { deletedAt: null });
}

export function addUserViolation(username: string, violation: NewViolation): void {
    const change = readChanges()[username];
    writeChange(username, { ...change, violations: [...(change?.violations ?? []), violation] });
}
