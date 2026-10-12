export const MIN_PASSWORD_LENGTH = 8;
export const MAX_PASSWORD_SCORE = 4;
const STRONG_PASSWORD_LENGTH = 12;
const MAX_SCORE_BELOW_MIN_LENGTH = 1;

export function getPasswordScore(password: string): number {
    const checks = [
        password.length >= MIN_PASSWORD_LENGTH,
        /\p{L}/u.test(password) && /\d/.test(password),
        /[^\p{L}\d\s]/u.test(password),
        password.length >= STRONG_PASSWORD_LENGTH,
    ];
    const passedCount = checks.filter(Boolean).length;

    return password.length >= MIN_PASSWORD_LENGTH
        ? passedCount
        : Math.min(passedCount, MAX_SCORE_BELOW_MIN_LENGTH);
}
