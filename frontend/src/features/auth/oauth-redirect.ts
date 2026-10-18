const OAUTH_NEXT_PATH_KEY = "sqljudge:oauth-next-path";

export function saveOAuthNextPath(nextPath: string): void {
    try {
        sessionStorage.setItem(OAUTH_NEXT_PATH_KEY, nextPath);
    } catch {
        return;
    }
}

export function readOAuthNextPath(): string | undefined {
    try {
        return sessionStorage.getItem(OAUTH_NEXT_PATH_KEY) ?? undefined;
    } catch {
        return undefined;
    }
}
