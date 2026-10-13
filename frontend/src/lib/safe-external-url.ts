const ALLOWED_PROTOCOLS = ["http:", "https:"];

export function safeExternalUrl(url: string | undefined): string | null {
    if (!url || !URL.canParse(url)) {
        return null;
    }
    return ALLOWED_PROTOCOLS.includes(new URL(url).protocol) ? url : null;
}

export function displayUrl(url: string): string {
    const { hostname, pathname } = new URL(url);
    return `${hostname.replace(/^www\./, "")}${pathname.replace(/\/$/, "")}`;
}
