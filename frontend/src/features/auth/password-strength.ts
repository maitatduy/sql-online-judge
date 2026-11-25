export const MIN_PASSWORD_LENGTH = 8;
export const MAX_PASSWORD_SCORE = 4;
const STRONG_PASSWORD_LENGTH = 12;
const MAX_SCORE_BELOW_MIN_LENGTH = 1;

export type PasswordLevel = "EMPTY" | "WEAK" | "FAIR" | "STRONG";

export interface PasswordRule {
    label: string;
    isMet: boolean;
}

export function getPasswordRules(password: string): PasswordRule[] {
    return [
        {
            label: `Ít nhất ${MIN_PASSWORD_LENGTH} ký tự`,
            isMet: password.length >= MIN_PASSWORD_LENGTH,
        },
        {
            label: "Có cả chữ và số",
            isMet: /\p{L}/u.test(password) && /\d/.test(password),
        },
        { label: "Có ký tự đặc biệt", isMet: /[^\p{L}\d\s]/u.test(password) },
        {
            label: `Từ ${STRONG_PASSWORD_LENGTH} ký tự trở lên`,
            isMet: password.length >= STRONG_PASSWORD_LENGTH,
        },
    ];
}

export function getPasswordScore(password: string): number {
    const passedCount = getPasswordRules(password).filter((rule) => rule.isMet).length;

    return password.length >= MIN_PASSWORD_LENGTH
        ? passedCount
        : Math.min(passedCount, MAX_SCORE_BELOW_MIN_LENGTH);
}

export function getPasswordLevel(password: string): PasswordLevel {
    if (password === "") {
        return "EMPTY";
    }
    const score = getPasswordScore(password);
    if (score <= 1) {
        return "WEAK";
    }
    return score === 2 ? "FAIR" : "STRONG";
}
