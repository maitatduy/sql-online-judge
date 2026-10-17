export const RESEND_COOLDOWN_SECONDS = 60;
const SECONDS_PER_MINUTE = 60;

export function formatCountdown(totalSeconds: number): string {
    const minutes = Math.floor(totalSeconds / SECONDS_PER_MINUTE);
    const seconds = totalSeconds % SECONDS_PER_MINUTE;
    return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
}
