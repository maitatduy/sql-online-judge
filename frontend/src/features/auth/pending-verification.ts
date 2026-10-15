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
