import { ROUTES } from "@/lib/routes";
import { getSafeNextPath } from "@/lib/safe-redirect";

const PENDING_EMAIL_KEY = "sqljudge:pending-verification-email";

export function savePendingVerificationEmail(email: string): void {
    try {
        sessionStorage.setItem(PENDING_EMAIL_KEY, email);
    } catch {
        return;
    }
}

export function readPendingVerificationEmail(): string | null {
    try {
        return sessionStorage.getItem(PENDING_EMAIL_KEY);
    } catch {
        return null;
    }
}

const PENDING_NEXT_PATH_KEY = "sqljudge:pending-verification-next-path";

export function savePendingNextPath(nextPath: string): void {
    try {
        localStorage.setItem(PENDING_NEXT_PATH_KEY, nextPath);
    } catch {
        return;
    }
}

export function takePendingNextPath(): string {
    try {
        const nextPath = localStorage.getItem(PENDING_NEXT_PATH_KEY) ?? undefined;
        localStorage.removeItem(PENDING_NEXT_PATH_KEY);
        return getSafeNextPath(nextPath);
    } catch {
        return ROUTES.home;
    }
}
