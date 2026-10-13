const MAX_INITIALS = 2;
const FALLBACK_INITIALS = "?";

export function initialsOf(displayName: string): string {
    const words = displayName.trim().split(/\s+/).filter(Boolean);
    if (words.length === 0) {
        return FALLBACK_INITIALS;
    }
    return words
        .slice(-MAX_INITIALS)
        .map((word) => word.charAt(0).toLocaleUpperCase("vi-VN"))
        .join("");
}
