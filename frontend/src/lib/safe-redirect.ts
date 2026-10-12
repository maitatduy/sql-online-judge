import { ROUTES } from "@/lib/routes";

const PLACEHOLDER_ORIGIN = "http://internal.invalid";
const FIRST_PRINTABLE_CHAR_CODE = 32;
const DELETE_CHAR_CODE = 127;

function hasControlCharacter(value: string): boolean {
    return Array.from(value).some((character) => {
        const code = character.charCodeAt(0);
        return code < FIRST_PRINTABLE_CHAR_CODE || code === DELETE_CHAR_CODE;
    });
}

function resolveInternalPath(path: string): string | null {
    try {
        const resolved = new URL(path, PLACEHOLDER_ORIGIN);
        return resolved.origin === PLACEHOLDER_ORIGIN
            ? `${resolved.pathname}${resolved.search}${resolved.hash}`
            : null;
    } catch {
        return null;
    }
}

export function getSafeNextPath(next: string | string[] | undefined): string {
    if (typeof next !== "string" || !next.startsWith("/")) {
        return ROUTES.home;
    }
    if (next.includes("\\") || hasControlCharacter(next)) {
        return ROUTES.home;
    }
    return resolveInternalPath(next) ?? ROUTES.home;
}
