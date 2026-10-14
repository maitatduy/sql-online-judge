import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type { UserNote } from "@/features/admin/users/types";

import { userNotFound, validationFailed } from "./admin-user-errors";
import { findAdminUser, findAnyAdminUser } from "./admin-user-store";
import { readMockSession } from "./session";

const NOTES_STORAGE_KEY = "sqljudge:mock:admin-user-notes";
const MAX_NOTE_LENGTH = 1000;
const CREATED_STATUS = 201;

const SEEDED_NOTES: Record<string, UserNote[]> = {
    "viet.luu": [
        {
            id: "viet.luu-n1",
            authorUsername: "mod.thu",
            body: "Đã nhắc nhở qua tin nhắn, người dùng hứa không tái phạm.",
            createdAt: "2026-09-12T03:00:00Z",
        },
    ],
};

function readAddedNotes(): Record<string, UserNote[]> {
    const stored = sessionStorage.getItem(NOTES_STORAGE_KEY);
    return stored ? (JSON.parse(stored) as Record<string, UserNote[]>) : {};
}

function notesOf(username: string): UserNote[] {
    return [...(readAddedNotes()[username] ?? []), ...(SEEDED_NOTES[username] ?? [])];
}

function noteBodyError(body: unknown): string | null {
    if (typeof body !== "string" || body.trim() === "") {
        return "Không được để trống";
    }
    return body.trim().length > MAX_NOTE_LENGTH ? `Tối đa ${MAX_NOTE_LENGTH} ký tự` : null;
}

export const adminUserNoteHandlers: RequestHandler[] = [
    http.get("*/api/admin/users/:username/notes", async ({ params }) => {
        await delay();
        const user = findAnyAdminUser(String(params.username));
        return user ? HttpResponse.json({ items: notesOf(user.username) }) : userNotFound();
    }),

    http.post("*/api/admin/users/:username/notes", async ({ params, request }) => {
        await delay();
        const username = String(params.username);
        const { body } = (await request.json()) as { body?: unknown };
        const invalidBody = noteBodyError(body);
        if (invalidBody) {
            return validationFailed({ field: "body", message: invalidBody });
        }
        if (!findAdminUser(username)) {
            return userNotFound();
        }
        const added = readAddedNotes();
        const note: UserNote = {
            id: `${username}-n${Date.now()}`,
            authorUsername: readMockSession()?.username ?? "admin",
            body: String(body).trim(),
            createdAt: new Date().toISOString(),
        };
        sessionStorage.setItem(
            NOTES_STORAGE_KEY,
            JSON.stringify({ ...added, [username]: [note, ...(added[username] ?? [])] }),
        );
        return HttpResponse.json(note, { status: CREATED_STATUS });
    }),
];
